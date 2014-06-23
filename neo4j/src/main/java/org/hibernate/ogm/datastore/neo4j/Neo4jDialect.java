/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.CypherCRUD.relationshipType;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.TEMP_NODE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.CypherCRUD;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.MapsTupleIterator;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTypeConverter;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodesTupleIterator;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.query.impl.Neo4jParameterMetadataBuilder;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.AssociationOperation;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKind;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
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
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jDialect implements GridDialect, ServiceRegistryAwareService {

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

	private static Tuple createTuple(Node entityNode) {
		return new Tuple( new Neo4jTupleSnapshot( entityNode ) );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return createTuple( neo4jCRUD.createNodeUnlessExists( key, ENTITY ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		Node node = (Node) ( (Neo4jTupleSnapshot) tuple.getSnapshot() ).getPropertyContainer();
		applyTupleOperations( node, tuple.getOperations() );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		neo4jCRUD.remove( key );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		PropertyContainer property = createRelationshipToEntityOrToTempNode( associationKey, rowKey );
		return new Tuple( new Neo4jTupleSnapshot( property ) );
	}

	/**
	 * When dealing with some scenarios like, for example, a bidirectional association, OGM calls this method twice:
	 * <p>
	 * the first time with the information related to the owner of the association and the {@link RowKey},
	 * the second time using the same {@link RowKey} but with the {@link AssociationKey} referring to the other side of the association.
	 * <p>
	 * What happen in this method is that the first time I'm going to save the {@link RowKey} information in a temporary
	 * node and the second time I'm going to delete the node and connect the two entities with two relationships.
	 * <p>
	 * This approach works at the moment because:
	 * <ol>
	 * <li>everything is inside a transaction
	 * <li>a given session is not concurrent and execute operation sequentially
	 * <li>the method is called a second time **right after** the first time
	 * </ol>
	 * So the same RowKey cannot be created for two different associations at the same time from within the same
	 * transaction.
	 */
	private PropertyContainer createRelationshipToEntityOrToTempNode(AssociationKey associationKey, RowKey rowKey) {
		Node rowKeyNode = neo4jCRUD.findNode( rowKey );
		// Check if there is an entity or a temporary node representing the RowKey
		if ( rowKeyNode == null ) {
			if ( associationKey.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION ) {
				return createNodeAndAddRelationship( associationKey, rowKey, NodeLabel.EMBEDDED );
			}
			else {
				// We look for the entity at the end of the association, if we cannot find it
				// we save the RowKey in a temporary node.
				return findEntityOrCreateTempNode( associationKey, rowKey );
			}
		}
		else if ( rowKeyNode.hasLabel( ENTITY ) ) {
			// The RowKey represents an entity and we are going to create the relationship to it
			return createRelationshipWithEntity( associationKey, rowKey, rowKeyNode );
		}
		else if ( rowKeyNode.hasLabel( TEMP_NODE ) ) {
			// We have found a temporary node related to this association, we are going to delete it and connect the
			// entity pointing to the temporary node and the owner of this association.
			return deleteTempNodeAndCreateRelationshipWithEntity( associationKey, rowKey, rowKeyNode );
		}
		else {
			throw new AssertionFailure( "Unrecognized row key node: " + rowKeyNode );
		}
	}

	private PropertyContainer findEntityOrCreateTempNode(AssociationKey associationKey, RowKey rowKey) {
		EntityKey endNodeKey = endNodeKey( associationKey, rowKey );
		Node endNode = neo4jCRUD.findNode( endNodeKey, ENTITY );
		if ( endNode == null ) {
			// We cannot find the entity on the other side of the relationship, we store the information related to
			// the RowKey in a temporary node and we create a relationship to it
			return createNodeAndAddRelationship( associationKey, rowKey, TEMP_NODE );
		}
		else if ( associationKey.getCollectionRole().equals( rowKey.getTable() ) ) {
			// Unidirectional ManyToOne: the node contains the field with the association
			// TODO: there should be a relationship in this case
			return endNode;
		}
		else {
			// Bidirectional ManyToOne: the node contains the field with the association.
			// I'll create the relationship between the owner and the end node
			return createRelationshipWithEntity( associationKey, rowKey, endNode );
		}
	}

	/**
	 * This method returns the {@link EntityKey} that represents the entity on the other side of the relationship.
	 * <p>
	 * At the moment the {@link AssociationKey} contains the owner of the association but it is missing the information
	 * related to the entity on the other side of the association. To obtain it, we remove from {@link RowKey} the
	 * columns in AssociationKey, the remaining ones should represent the identifier at the end of the association.
	 * <p>
	 * For List, Map and persistent collections with identifiers, the remaining columns are not the other side
	 * identifier but rather then index, key or surrogate identifier. This node does not exist and will always return
	 * null.
	 * <p>
	 * TODO: use metadata to avoid this unnecessary lookup in that case.
	 */
	private EntityKey endNodeKey(AssociationKey associationKey, RowKey rowKey) {
		List<String> keyColumnNames = new ArrayList<String>();
		List<Object> keyColumnValues = new ArrayList<Object>();
		String[] columnNames = rowKey.getColumnNames();
		int i = 0;
		for ( String columnName : columnNames ) {
			boolean entityColumn = true;
			for ( String associationColumnName : associationKey.getColumnNames() ) {
				if ( associationColumnName.equals( columnName ) ) {
					entityColumn = false;
					break;
				}
			}
			if ( entityColumn ) {
				keyColumnNames.add( columnName );
				keyColumnValues.add( rowKey.getColumnValues()[i] );
			}
			i++;
		}
		return new EntityKey( new EntityKeyMetadata( associationKey.getTable(), keyColumnNames.toArray( new String[keyColumnNames.size()] ) ),
				keyColumnValues.toArray( new Object[keyColumnValues.size()] ) );
	}

	private Relationship deleteTempNodeAndCreateRelationshipWithEntity(AssociationKey associationKey, RowKey rowKey, Node tempNode) {
		Node ownerNode = neo4jCRUD.findNode( associationKey.getEntityKey(), ENTITY );
		Iterator<Relationship> iterator = tempNode.getRelationships( Direction.INCOMING ).iterator();
		Relationship tempRelationship = iterator.next();
		Relationship relationship = ownerNode.createRelationshipTo( tempRelationship.getStartNode(), relationshipType( associationKey ) );
		applyColumnValues( rowKey, relationship );
		tempRelationship.delete();
		tempNode.delete();
		return relationship;
	}

	private PropertyContainer createRelationshipWithEntity(AssociationKey associationKey, RowKey rowKey, Node node) {
		Node ownerNode = neo4jCRUD.findNode( associationKey.getEntityKey(), ENTITY );
		Relationship relationship = ownerNode.createRelationshipTo( node, relationshipType( associationKey ) );
		applyColumnValues( rowKey, relationship );
		return relationship;
	}

	private PropertyContainer createNodeAndAddRelationship(AssociationKey associationKey, RowKey rowKey, NodeLabel label) {
		Node rowKeyNode = neo4jCRUD.createNodeUnlessExists( rowKey, label );
		return createRelationshipWithEntity( associationKey, rowKey, rowKeyNode );
	}

	private void applyColumnValues(RowKey rowKey, PropertyContainer relationship) {
		for ( int i = 0; i < rowKey.getColumnNames().length; i++ ) {
			// Neo4j does not support null values but in the embedded case it might happen to have some nulls
			if ( rowKey.getColumnValues()[i] != null ) {
				relationship.setProperty( rowKey.getColumnNames()[i], rowKey.getColumnValues()[i] );
			}
		}
	}

	@Override
	public Association getAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		Node entityNode = neo4jCRUD.findNode( associationKey.getEntityKey(), ENTITY );
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
		for ( AssociationOperation action : association.getOperations() ) {
			applyAssociationOperation( key, action, associationContext );
		}
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
		return false;
	}

	@Override
	public void nextValue(IdGeneratorKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		int nextValue = neo4jSequenceGenerator.nextValue( key, increment );
		value.initialize( nextValue );
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
			neo4jCRUD.remove( key );
		}
	}

	private void applyAssociationOperation(AssociationKey key, AssociationOperation operation, AssociationContext associationContext) {
		switch ( operation.getType() ) {
		case CLEAR:
			removeAssociation( key, associationContext );
			break;
		case PUT:
			putAssociationOperation( key, operation );
			break;
		case PUT_NULL:
			removeAssociationOperation( key, operation );
			break;
		case REMOVE:
			removeAssociationOperation( key, operation );
			break;
		}
	}

	private void putAssociationOperation(AssociationKey associationKey, AssociationOperation action) {
		Relationship relationship = neo4jCRUD.findRelationship( associationKey, action.getKey() );
		if ( relationship != null ) {
			applyTupleOperations( relationship, action.getValue().getOperations() );
		}
	}

	private void removeAssociationOperation(AssociationKey associationKey, AssociationOperation action) {
		neo4jCRUD.remove( associationKey, action.getKey() );
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
			removeTupleOperation( node, operation );
			break;
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
	public ClosableIterator<Tuple> executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters) {
		Map<String, Object> parameters = getNamedParameterValuesConvertedByGridType( queryParameters );

		String nativeQuery = customQuery.getQueryString();
		ExecutionResult result = neo4jCRUD.executeQuery( nativeQuery, parameters );

		if ( customQuery.getSingleEntityKeyMetadataOrNull() != null ) {
			return new NodesTupleIterator( result );
		}
		return new MapsTupleIterator( result );
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
