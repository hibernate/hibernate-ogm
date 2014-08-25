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
import java.util.HashSet;
import java.util.Iterator;
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
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTypeConverter;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodesTupleIterator;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.logging.impl.GraphLogger;
import org.hibernate.ogm.datastore.neo4j.query.impl.Neo4jParameterMetadataBuilder;
import org.hibernate.ogm.datastore.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.AssociationOperation;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.QueryableGridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
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
		return createTuple( entityNode, context );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		Node node = neo4jCRUD.createNodeUnlessExists( key, ENTITY );
		GraphLogger.log( "Created node: %1$s", node );
		return createTuple( node, tupleContext );
	}

	private static Tuple createTuple(Node entityNode) {
		return new Tuple( new Neo4jTupleSnapshot( entityNode ) );
	}

	private static Tuple createTuple(Node entityNode, TupleContext tupleContext) {
		return new Tuple( new Neo4jTupleSnapshot( entityNode, tupleContext.getAllAssociatedEntityKeyMetadata(), tupleContext.getAllRoles() ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		Neo4jTupleSnapshot snapshot = (Neo4jTupleSnapshot) tuple.getSnapshot();
		Node node = snapshot.getNode();
		applyTupleOperations( tuple, node, tuple.getOperations(), tupleContext );
		GraphLogger.log( "Updated node: %1$s", node );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		neo4jCRUD.remove( key );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple();
	}

	/**
	 * When dealing with some scenarios like, for example, a bidirectional association, OGM calls this method twice:
	 * <p>
	 * the first time with the information related to the owner of the association and the {@link RowKey},
	 * the second time using the same {@link RowKey} but with the {@link AssociationKey} referring to the other side of the association.
	 */
	private Relationship createRelationship(AssociationKey associationKey, RowKey rowKey, EntityKey targetKey) {
		switch ( associationKey.getAssociationKind() ) {
			case EMBEDDED_COLLECTION:
				return createRelationshipWithEmbeddedNode( associationKey, rowKey, targetKey );
			case ASSOCIATION:
				return findOrCreateRelationshipWithEntityNode( associationKey, rowKey, targetKey );
			default:
				throw new AssertionFailure( "Unrecognized associationKind: " + associationKey.getAssociationKind() );
		}
	}

	private Relationship createRelationshipWithEmbeddedNode(AssociationKey associationKey, RowKey rowKey, EntityKey targetKey) {
		Node embeddedNode = neo4jCRUD.createNode( targetKey, EMBEDDED );
		Relationship relationship = createRelationshipWithTargetNode( associationKey, rowKey, embeddedNode );
		applyProperties( associationKey, rowKey, relationship );
		return relationship;
	}

	private Relationship findOrCreateRelationshipWithEntityNode(AssociationKey associationKey, RowKey rowKey, EntityKey targetKey) {
		Relationship relationship = neo4jCRUD.findRelationship( associationKey, rowKey );
		if ( relationship != null ) {
			return relationship;
		}

		Node targetNode = neo4jCRUD.findNode( targetKey, ENTITY );
		return createRelationshipWithTargetNode( associationKey, rowKey, targetNode );
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

	private Relationship createRelationshipWithTargetNode(AssociationKey associationKey, RowKey rowKey, Node targetNode) {
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
		return new Association( new Neo4jAssociationSnapshot( entityNode, associationKey ) );
	}

	@Override
	public Association createAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		return new Association();
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		// If this is the inverse side of a bi-directional association, we don't create a relationship for this; this
		// will happen when updating the main side
		if ( key.getMetadata().isInverse() ) {
			return;
		}

		for ( AssociationOperation action : association.getOperations() ) {
			applyAssociationOperation( association, key, action, associationContext );
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
		// If this is the inverse side of a bi-directional association, we don't manage the relationship from this side
		if ( key.getMetadata().isInverse() ) {
			return;
		}

		neo4jCRUD.remove( key );
	}

	private void applyAssociationOperation(Association association, AssociationKey key, AssociationOperation operation, AssociationContext associationContext) {
		switch ( operation.getType() ) {
		case CLEAR:
			removeAssociation( key, associationContext );
			break;
		case PUT:
			putAssociationOperation( association, key, operation );
			break;
		case PUT_NULL:
			removeAssociationOperation( association, key, operation );
			break;
		case REMOVE:
			removeAssociationOperation( association, key, operation );
			break;
		}
	}

	private void putAssociationOperation(Association association, AssociationKey associationKey, AssociationOperation action) {
		Relationship relationship = neo4jCRUD.findRelationship( associationKey, action.getKey() );

		if (relationship != null) {
			for ( String relationshipProperty : associationKey.getMetadata().getRowKeyIndexColumnNames() ) {
				relationship.setProperty( relationshipProperty, action.getValue().get( relationshipProperty ) );

			}
			GraphLogger.log( "Updated relationship: %1$s", relationship );
		}
		else {
			EntityKey targetKey = association.getTargetKey( action.getKey(), associationKey.getMetadata() );
			relationship = createRelationship( associationKey, action.getKey(), targetKey );

			GraphLogger.log( "Created relationship: %1$s", relationship );
		}
	}

	private void removeAssociationOperation(Association association, AssociationKey associationKey, AssociationOperation action) {
		neo4jCRUD.remove( associationKey, action.getKey() );
	}

	private void applyTupleOperations(Tuple tuple, Node node, Set<TupleOperation> operations, TupleContext tupleContext) {
		Set<String> processedAssociationRoles = new HashSet<String>();

		for ( TupleOperation operation : operations ) {
			applyOperation( tuple, node, operation, tupleContext, processedAssociationRoles );
		}
	}

	private void applyOperation(Tuple tuple, Node node, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		switch ( operation.getType() ) {
		case PUT:
			putTupleOperation( tuple, node, operation, tupleContext, processedAssociationRoles );
			break;
		case PUT_NULL:
			removeTupleOperation( node, operation, tupleContext, processedAssociationRoles );
			break;
		case REMOVE:
			removeTupleOperation( node, operation, tupleContext, processedAssociationRoles );
			break;
		}
	}

	private void removeTupleOperation(Node node, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		if ( !tupleContext.isPartOfAssociation( operation.getColumn() ) ) {
			if ( node.hasProperty( operation.getColumn() ) ) {
				node.removeProperty( operation.getColumn() );
			}
		}
		// if the column represents a to-one association, remove the relationship
		else {
			String associationRole = tupleContext.getRole( operation.getColumn() );
			if ( !processedAssociationRoles.contains( associationRole ) ) {

				Iterator<Relationship> relationships = node.getRelationships( relationshipType( associationRole ) ).iterator();

				if ( relationships.hasNext() ) {
					relationships.next().delete();
				}
			}
		}
	}

	private void putTupleOperation(Tuple tuple, Node node, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		if ( !tupleContext.isPartOfAssociation( operation.getColumn() ) ) {
			node.setProperty( operation.getColumn(), operation.getValue() );
		}
		// the column represents a to-one association, map it as relationship
		else {
			String associationRole = tupleContext.getRole( operation.getColumn() );

			if ( !processedAssociationRoles.contains( associationRole ) ) {
				processedAssociationRoles.add( associationRole );

				EntityKey targetKey = tupleContext.getAssociatedEntityKeyMetadata( operation.getColumn() ).getEntityKey( tuple );

				// delete the previous relationship if there is one; for a to-one association, the relationship won't have any
				// properties, so the type is uniquely identifying it
				Iterator<Relationship> relationships = node.getRelationships( relationshipType( associationRole ) ).iterator();
				if ( relationships.hasNext() ) {
					relationships.next().delete();
				}

				// create a new relationship
				Node targetNode = neo4jCRUD.findNode( targetKey , ENTITY );
				node.createRelationshipTo( targetNode, relationshipType( associationRole ) );
			}
		}
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
