/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.CypherCRUD.relationshipType;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.EMBEDDED;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.limit;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.skip;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.CypherCRUD;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.MapsTupleIterator;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTupleAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTypeConverter;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodesTupleIterator;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.logging.impl.GraphLogger;
import org.hibernate.ogm.datastore.neo4j.query.impl.Neo4jParameterMetadataBuilder;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.AssociationOperation;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.QueryableGridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKind;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.Key;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.id.spi.NextValueRequest;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.spi.BackendQuery;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;

/**
 * Abstracts Hibernate OGM from Neo4j.
 * <p>
 * A {@link Tuple} is saved as a {@link Node} where the columns are converted into properties of the node.<br>
 * An {@link Association} is converted into a {@link Relationship} identified by the {@link AssociationKey} and the
 * {@link RowKey}. The type of the relationship is the value returned by {@link AssociationKey#getCollectionRole()}.
 * <p>
 * If the value of a property is set to null the property will be removed (Neo4j does not allow to store null values).
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jDialect extends BaseGridDialect implements QueryableGridDialect<String>, ServiceRegistryAwareService {

	private static final Object[] EMPTY_VALUES_ARRAY = new Object[0];

	private static final String[] EMPTY_COLUMN_ARRAY = new String[0];

	private final CypherCRUD neo4jCRUD;

	private final Neo4jSequenceGenerator neo4jSequenceGenerator;

	private ServiceRegistryImplementor serviceRegistry;

	public Neo4jDialect(Neo4jDatastoreProvider provider) {
		this.neo4jCRUD = new CypherCRUD( provider.getDataBase() );
		this.neo4jSequenceGenerator = provider.getSequenceGenerator();
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		throw new UnsupportedOperationException( "LockMode " + lockMode + " is not supported by the Neo4j GridDialect" );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext context) {
		Node entityNode = neo4jCRUD.findNode( key, ENTITY );
		if ( entityNode == null ) {
			return null;
		}
		return createTuple( entityNode );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		Node node = neo4jCRUD.createNodeUnlessExists( key, ENTITY );
		GraphLogger.log( "Created node: %1$s", node );
		return createTuple( node );
	}

	private static Tuple createTuple(Node entityNode) {
		return new Tuple( new Neo4jTupleSnapshot( entityNode ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		Neo4jTupleSnapshot snapshot = (Neo4jTupleSnapshot) tuple.getSnapshot();
		Node node = snapshot.getNode();
		applyTupleOperations( node, tuple.getOperations() );
		GraphLogger.log( "Updated node: %1$s", node );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		neo4jCRUD.remove( key );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, AssociationContext associationContext, RowKey rowKey) {
		Relationship relationship = createRelationship( associationKey, associationContext, rowKey );
		GraphLogger.log( "Relationship: %1$s", relationship );
		if ( relationship == null ) {
			// This should only happen for bidirectional associations, when we are creating the association on the owner side.
			// We can ignore the creation of the relationship in this case and we will create it when dealing with the inverese side of
			// the same association
			return new Tuple();
		}
		return new Tuple( new Neo4jTupleAssociationSnapshot( relationship, associationKey, associationContext ) );
	}

	/**
	 * When dealing with some scenarios like, for example, a bidirectional association, OGM calls this method twice:
	 * <p>
	 * the first time with the information related to the owner of the association and the {@link RowKey},
	 * the second time using the same {@link RowKey} but with the {@link AssociationKey} referring to the other side of the association.
	 */
	private Relationship createRelationship(AssociationKey associationKey, AssociationContext associationContext, RowKey rowKey) {
		Node rowKeyNode = neo4jCRUD.findNode( rowKey );
		// Check if the RowKey represents an entity
		if ( rowKeyNode == null ) {
			// We create a relationship using the rowKey
			return convertRowKeyToRelationship( associationKey, associationContext, rowKey );
		}
		else if ( rowKeyNode.hasLabel( ENTITY ) ) {
			// CompositeId: The RowKey represents an entity and we are going to create the relationship to it
			return createRelationshipWithTargetNode( associationKey, associationContext, rowKey, rowKeyNode );
		}
		else {
			throw new AssertionFailure( "Unrecognized row key node: " + rowKeyNode );
		}
	}

	private Relationship convertRowKeyToRelationship(AssociationKey associationKey, AssociationContext associationContext, RowKey rowKey) {
		switch ( associationKey.getAssociationKind() ) {
			case EMBEDDED_COLLECTION:
				return createRelationshipWithEmbeddedNode( associationKey, associationContext, rowKey );
			case ASSOCIATION:
				return findOrCreateRelationshipWithEntityNode( associationKey, associationContext, rowKey );
			default:
				throw new AssertionFailure( "Unrecognized associationKind: " + associationKey.getAssociationKind() );
		}
	}

	private Relationship createRelationshipWithEmbeddedNode(AssociationKey associationKey, AssociationContext associationContext, RowKey rowKey) {
		Key key = targetKey( associationKey, associationContext, rowKey );
		Node embeddedNode = neo4jCRUD.createNode( key, EMBEDDED );
		Relationship relationship = createRelationshipWithTargetNode( associationKey, associationContext, rowKey, embeddedNode );
		applyProperties( associationKey, rowKey, relationship );
		return relationship;
	}

	/**
	 * Returns the key that identify the entity on the target side of the association. It might not be possible to
	 * create the key if the association key and the row key refer to the owner side of the association.
	 */
	public static Key targetKey(AssociationKey associationKey, AssociationContext associationContext, RowKey rowKey) {
		if ( isEmbeddedWithIndex( associationKey ) ) {
			// The embedded collection has an index, I don't need to know the column names or values of the target.
			// It is going to be identified by the index on the relationship,
			return targeKeyForEmbeddedWithIndex( associationKey );
		}
		else {
			return targetKeyForAssociationOrEmbedded( associationContext, rowKey );
		}
	}

	private static EntityKey targeKeyForEmbeddedWithIndex(AssociationKey associationKey) {
		return new EntityKey( new EntityKeyMetadata( associationKey.getTable(), EMPTY_COLUMN_ARRAY ), EMPTY_VALUES_ARRAY );
	}

	private static boolean isEmbeddedWithIndex(AssociationKey associationKey) {
		return AssociationKind.EMBEDDED_COLLECTION == associationKey.getAssociationKind()
				&& associationKey.getMetadata().getRowKeyIndexColumnNames().length > 0;
	}

	private static Key targetKeyForAssociationOrEmbedded(AssociationContext associationContext, RowKey rowKey) {
		String[] targetKeyColumnNames = associationContext.getTargetEntityKeyMetadata().getColumnNames();
		Object[] targetKeyColumnValues = new Object[targetKeyColumnNames.length];
		String[] associationTargetColumnNames = associationContext.getTargetAssociationKeyMetadata().getColumnNames();
		for ( int i = 0; i < associationTargetColumnNames.length; i++ ) {
			if ( rowKey.contains( associationTargetColumnNames[i] ) ) {
				targetKeyColumnValues[i] = rowKey.getColumnValue( associationTargetColumnNames[i] );
			}
			else {
				// The RowKey does not contain the value of the target side of the association, it means we are on the
				// owner side.
				return null;
			}
		}
		return new EntityKey( associationContext.getTargetEntityKeyMetadata(), targetKeyColumnValues );
	}

	private Relationship findOrCreateRelationshipWithEntityNode(AssociationKey associationKey, AssociationContext associationContext, RowKey rowKey) {
		Key targetKey = targetKey( associationKey, associationContext, rowKey );
		if ( targetKey == null ) {
			// We have to wait the creation of the target side of the association before
			// we can obtain the targetKey
			return null;
		}

		Relationship relationship = neo4jCRUD.findRelationship( associationKey, associationContext, rowKey, targetKey );
		if ( relationship != null ) {
			return relationship;
		}

		Node targetNode = neo4jCRUD.findNode( targetKey, ENTITY );
		return createRelationshipWithTargetNode( associationKey, associationContext, rowKey, targetNode );
	}

	/**
	 * The only properties added to a relationship are the columns representing the index of the association.
	 */
	private void applyProperties(AssociationKey associationKey, RowKey rowKey, Relationship relationship) {
		String[] indexColumns = associationKey.getMetadata().getRowKeyIndexColumnNames();
		for ( int i = 0; i < indexColumns.length; i++ ) {
			String propertyName = indexColumns[i];
			Object propertyValue = rowKey.getColumnValue( propertyName );
			relationship.setProperty( propertyName, propertyValue );
		}
	}

	private Relationship createRelationshipWithTargetNode(AssociationKey associationKey, AssociationContext associationContext, RowKey rowKey, Node targetNode) {
		Node ownerNode = neo4jCRUD.findNode( associationKey.getEntityKey(), ENTITY );
		Relationship relationship = ownerNode.createRelationshipTo( targetNode, relationshipType( associationKey ) );
		applyProperties( associationKey, rowKey, relationship );
		return relationship;
	}

	@Override
	public Association getAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		Node entityNode = neo4jCRUD.findNode( associationKey.getEntityKey(), ENTITY );
		GraphLogger.log( "Found owner node: %1$s", entityNode );
		if ( entityNode == null ) {
			return null;
		}
		return new Association( new Neo4jAssociationSnapshot( entityNode, associationKey, associationContext ) );
	}

	@Override
	public Association createAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		return new Association();
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		for ( AssociationOperation action : association.getOperations() ) {
			applyAssociationOperation( key, associationContext, action );
		}
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return neo4jSequenceGenerator.nextValue( request.getKey(), request.getIncrement() );
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public GridType overrideType(Type type) {
		return Neo4jTypeConverter.INSTANCE.convert( type );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		if ( key != null ) {
			neo4jCRUD.remove( key, associationContext );
		}
	}

	private void applyAssociationOperation(AssociationKey key, AssociationContext associationContext, AssociationOperation operation) {
		switch ( operation.getType() ) {
		case CLEAR:
			removeAssociation( key, associationContext );
			break;
		case PUT:
			putAssociationOperation( key, associationContext, operation );
			break;
		case PUT_NULL:
		case REMOVE:
			removeAssociationOperation( key, associationContext, operation );
			break;
		}
	}

	private void putAssociationOperation(AssociationKey associationKey, AssociationContext associationContext, AssociationOperation action) {
		if ( associationKey.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ) {
			Key targetKey = targetKey( associationKey, associationContext, action.getKey() );
			Relationship relationship = neo4jCRUD.findRelationship( associationKey, associationContext, action.getKey(), targetKey );
			if (relationship != null) {
				for ( TupleOperation operation : action.getValue().getOperations() ) {
					if ( !contains( associationKey.getMetadata().getRowKeyColumnNames(), operation.getColumn() ) ) {
						applyOperation( relationship.getEndNode(), operation );
					}
				}
				GraphLogger.log( "Updated relationship: %1$s", relationship );
			}
		}
	}

	private boolean contains(String[] columnNames, String column) {
		for ( String each : columnNames ) {
			if ( each.equals( column ) ) {
				return true;
			}
		}
		return false;
	}

	private void removeAssociationOperation(AssociationKey associationKey, AssociationContext associationContext, AssociationOperation action) {
		Key targetKey = targetKey( associationKey, associationContext, action.getKey() );
		neo4jCRUD.remove( associationKey, associationContext, action.getKey(), targetKey );
	}

	private void applyTupleOperations(PropertyContainer propertyContainer, Set<TupleOperation> operations) {
		for ( TupleOperation operation : operations ) {
			applyOperation( propertyContainer, operation );
		}
	}

	private void applyOperation(PropertyContainer node, TupleOperation operation) {
		switch ( operation.getType() ) {
		case PUT:
			putTupleOperation( node, operation );
			break;
		case PUT_NULL:
		case REMOVE:
			removeTupleOperation( node, operation );
			break;
		}
	}

	private void removeTupleOperation(PropertyContainer node, TupleOperation operation) {
		if ( node.hasProperty( operation.getColumn() ) ) {
			node.removeProperty( operation.getColumn() );
		}
	}

	private void putTupleOperation(PropertyContainer node, TupleOperation operation) {
		node.setProperty( operation.getColumn(), operation.getValue() );
	}

	@Override
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			ResourceIterator<Node> queryNodes = neo4jCRUD.findNodes( entityKeyMetadata.getTable() );
			try {
				while ( queryNodes.hasNext() ) {
					Node next = queryNodes.next();
					Tuple tuple = createTuple( next );
					consumer.consume( tuple );
				}
			}
			finally {
				queryNodes.close();
			}
		}
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<String> backendQuery, QueryParameters queryParameters) {
		Map<String, Object> parameters = getNamedParameterValuesConvertedByGridType( queryParameters );
		String nativeQuery = buildNativeQuery( backendQuery, queryParameters );
		ExecutionResult result = neo4jCRUD.executeQuery( nativeQuery, parameters );

		if ( backendQuery.getSingleEntityKeyMetadataOrNull() != null ) {
			return new NodesTupleIterator( result );
		}
		return new MapsTupleIterator( result );
	}

	@Override
	public String parseNativeQuery(String nativeQuery) {
		// We return given Cypher queries as they are; Currently there is no API for validating Cypher queries without
		// actually executing them (see https://github.com/neo4j/neo4j/issues/2766)
		return nativeQuery;
	}

	private String buildNativeQuery(BackendQuery<String> customQuery, QueryParameters queryParameters) {
		StringBuilder nativeQuery = new StringBuilder( customQuery.getQuery() );
		applyFirstRow( queryParameters, nativeQuery );
		applyMaxRows( queryParameters, nativeQuery );
		return nativeQuery.toString();
	}

	private void applyFirstRow(QueryParameters queryParameters, StringBuilder nativeQuery) {
		Integer firstRow = queryParameters.getRowSelection().getFirstRow();
		if ( firstRow != null ) {
			skip( nativeQuery, firstRow );
		}
	}

	private void applyMaxRows(QueryParameters queryParameters, StringBuilder nativeQuery) {
		Integer maxRows = queryParameters.getRowSelection().getMaxRows();
		if ( maxRows != null ) {
			limit( nativeQuery, maxRows );
		}
	}

	/**
	 * Returns a map with the named parameter values from the given parameters object, converted by the {@link GridType}
	 * corresponding to each parameter type.
	 */
	private Map<String, Object> getNamedParameterValuesConvertedByGridType(QueryParameters queryParameters) {
		Map<String, Object> parameterValues = new HashMap<String, Object>( queryParameters.getNamedParameters().size() );
		Tuple dummy = new Tuple();
		TypeTranslator typeTranslator = serviceRegistry.getService( TypeTranslator.class );

		for ( Entry<String, TypedValue> parameter : queryParameters.getNamedParameters().entrySet() ) {
			GridType gridType = typeTranslator.getType( parameter.getValue().getType() );
			gridType.nullSafeSet( dummy, parameter.getValue().getValue(), new String[]{ parameter.getKey() }, null );
			parameterValues.put( parameter.getKey(), dummy.get( parameter.getKey() ) );
		}

		return parameterValues;
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return new Neo4jParameterMetadataBuilder();
	}
}
