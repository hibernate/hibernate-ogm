/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.limit;
import static org.hibernate.ogm.datastore.neo4j.query.parsing.cypherdsl.impl.CypherDSL.skip;
import static org.neo4j.graphdb.DynamicRelationshipType.withName;

import java.util.Collection;
import java.util.Collections;
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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.MapsTupleIterator;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jAssociationQueries;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jEntityQueries;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.Neo4jTypeConverter;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodesTupleIterator;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.logging.impl.GraphLogger;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.neo4j.query.impl.Neo4jParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.SessionFactoryLifecycleAwareDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationOperation;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.Type;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.kernel.impl.util.StringLogger;

/**
 * Abstracts Hibernate OGM from Neo4j.
 * <p>
 * A {@link Tuple} is saved as a {@link Node} where the columns are converted into properties of the node.<br>
 * An {@link Association} is converted into a {@link Relationship} identified by the {@link AssociationKey} and the
 * {@link RowKey}. The type of the relationship is the value returned by
 * {@link AssociationKeyMetadata#getCollectionRole()}.
 * <p>
 * If the value of a property is set to null the property will be removed (Neo4j does not allow to store null values).
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jDialect extends BaseGridDialect implements QueryableGridDialect<String>, ServiceRegistryAwareService, SessionFactoryLifecycleAwareDialect {

	private static final Log log = LoggerFactory.getLogger();

	private final Neo4jSequenceGenerator neo4jSequenceGenerator;

	private ServiceRegistryImplementor serviceRegistry;

	private Map<EntityKeyMetadata, Neo4jEntityQueries> entityQueries;

	private Map<AssociationKeyMetadata, Neo4jAssociationQueries> associationQueries;

	private final ExecutionEngine executionEngine;

	public Neo4jDialect(Neo4jDatastoreProvider provider) {
		this.executionEngine = new ExecutionEngine( provider.getDataBase(), StringLogger.SYSTEM_DEBUG );
		this.neo4jSequenceGenerator = provider.getSequenceGenerator();
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void sessionFactoryCreated(SessionFactoryImplementor sessionFactoryImplementor) {
		this.associationQueries = Collections.unmodifiableMap( initializeAssociationQueries( sessionFactoryImplementor ) );
		this.entityQueries = Collections.unmodifiableMap( initializeEntityQueries( sessionFactoryImplementor, associationQueries ) );
	}

	private Map<EntityKeyMetadata, Neo4jEntityQueries> initializeEntityQueries(SessionFactoryImplementor sessionFactoryImplementor,
			Map<AssociationKeyMetadata, Neo4jAssociationQueries> associationQueries) {
		Map<EntityKeyMetadata, Neo4jEntityQueries> entityQueries = initializeEntityQueries( sessionFactoryImplementor );
		for ( AssociationKeyMetadata associationKeyMetadata : associationQueries.keySet() ) {
			EntityKeyMetadata entityKeyMetadata = associationKeyMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata();
			if ( !entityQueries.containsKey( entityKeyMetadata ) ) {
				// Embeddables metadata
				entityQueries.put( entityKeyMetadata, new Neo4jEntityQueries( entityKeyMetadata ) );
			}
		}
		return entityQueries;
	}

	private Map<EntityKeyMetadata, Neo4jEntityQueries> initializeEntityQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<EntityKeyMetadata, Neo4jEntityQueries> queryMap = new HashMap<EntityKeyMetadata, Neo4jEntityQueries>();
		Collection<EntityPersister> entityPersisters = sessionFactoryImplementor.getEntityPersisters().values();
		for ( EntityPersister entityPersister : entityPersisters ) {
			if (entityPersister instanceof OgmEntityPersister ) {
				OgmEntityPersister ogmEntityPersister = (OgmEntityPersister) entityPersister;
				queryMap.put( ogmEntityPersister.getEntityKeyMetadata(), new Neo4jEntityQueries( ogmEntityPersister.getEntityKeyMetadata() ) );
			}
		}
		return queryMap;
	}

	private Map<AssociationKeyMetadata, Neo4jAssociationQueries> initializeAssociationQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<AssociationKeyMetadata, Neo4jAssociationQueries> queryMap = new HashMap<AssociationKeyMetadata, Neo4jAssociationQueries>();
		Collection<CollectionPersister> collectionPersisters = sessionFactoryImplementor.getCollectionPersisters().values();
		for ( CollectionPersister collectionPersister : collectionPersisters ) {
			if ( collectionPersister instanceof OgmCollectionPersister ) {
				OgmCollectionPersister ogmCollectionPersister = (OgmCollectionPersister) collectionPersister;
				EntityKeyMetadata ownerEntityKeyMetadata = ( (OgmEntityPersister) ( ogmCollectionPersister.getOwnerEntityPersister() ) ).getEntityKeyMetadata();
				AssociationKeyMetadata associationKeyMetadata = ogmCollectionPersister.getAssociationKeyMetadata();
				queryMap.put( associationKeyMetadata, new Neo4jAssociationQueries( ownerEntityKeyMetadata, associationKeyMetadata ) );
			}
		}
		return queryMap;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		throw new UnsupportedOperationException( "LockMode " + lockMode + " is not supported by the Neo4j GridDialect" );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext context) {
		Node entityNode = entityQueries.get( key.getMetadata() ).findEntity( executionEngine, key.getColumnValues() );
		if ( entityNode == null ) {
			return null;
		}
		return createTuple( entityNode, context );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		return new Tuple();
	}

	private static Tuple createTuple(Node entityNode, TupleContext tupleContext) {
		return new Tuple( new Neo4jTupleSnapshot( entityNode, tupleContext.getAllAssociatedEntityKeyMetadata(), tupleContext.getAllRoles() ) );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		Node node = entityQueries.get( key.getMetadata() ).findOrCreateEntity( executionEngine, key.getColumnValues() );
		applyTupleOperations( key, tuple, node, tuple.getOperations(), tupleContext );
		GraphLogger.log( "Inserted/Updated node: %1$s", node );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		entityQueries.get( key.getMetadata() ).removeEntity( executionEngine, key.getColumnValues() );
	}

	/**
	 * When dealing with some scenarios like, for example, a bidirectional association, OGM calls this method twice:
	 * <p>
	 * the first time with the information related to the owner of the association and the {@link RowKey},
	 * the second time using the same {@link RowKey} but with the {@link AssociationKey} referring to the other side of the association.
	 * @param associatedEntityKeyMetadata
	 */
	private Relationship createRelationship(AssociationKey associationKey, Tuple associationRow, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		switch ( associationKey.getMetadata().getAssociationKind() ) {
			case EMBEDDED_COLLECTION:
				return createRelationshipWithEmbeddedNode( associationKey, associationRow, associatedEntityKeyMetadata );
			case ASSOCIATION:
				return findOrCreateRelationshipWithEntityNode( associationKey, associationRow, associatedEntityKeyMetadata );
			default:
				throw new AssertionFailure( "Unrecognized associationKind: " + associationKey.getMetadata().getAssociationKind() );
		}
	}

	private Relationship createRelationshipWithEmbeddedNode(AssociationKey associationKey, Tuple associationRow, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		EntityKey entityKey = getEntityKey( associationRow, associatedEntityKeyMetadata );
		Node embeddedNode = entityQueries.get( entityKey.getMetadata() ).createEmbedded( executionEngine, entityKey.getColumnValues() );
		Relationship relationship = createRelationshipWithTargetNode( associationKey, associationRow, embeddedNode );
		applyProperties( associationKey, associationRow, relationship );
		return relationship;
	}

	private Relationship findOrCreateRelationshipWithEntityNode(AssociationKey associationKey, Tuple associationRow, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		EntityKey targetEntityKey = getEntityKey( associationRow, associatedEntityKeyMetadata );
		Node targetNode = entityQueries.get( targetEntityKey.getMetadata() ).findEntity( executionEngine, targetEntityKey.getColumnValues() );
		return createRelationshipWithTargetNode( associationKey, associationRow, targetNode );
	}

	/**
	 * The only properties added to a relationship are the columns representing the index of the association.
	 */
	private void applyProperties(AssociationKey associationKey, Tuple associationRow, Relationship relationship) {
		String[] indexColumns = associationKey.getMetadata().getRowKeyIndexColumnNames();
		for ( int i = 0; i < indexColumns.length; i++ ) {
			String propertyName = indexColumns[i];
			Object propertyValue = associationRow.get( propertyName );
			relationship.setProperty( propertyName, propertyValue );
		}
	}

	private Relationship createRelationshipWithTargetNode(AssociationKey associationKey, Tuple associationRow, Node targetNode) {
		EntityKey entityKey = associationKey.getEntityKey();
		Node ownerNode = entityQueries.get( entityKey.getMetadata() ).findEntity( executionEngine, entityKey.getColumnValues() );
		Relationship relationship = ownerNode.createRelationshipTo( targetNode, withName( associationKey.getMetadata().getCollectionRole() ) );
		applyProperties( associationKey, associationRow, relationship );
		return relationship;
	}

	@Override
	public Association getAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		EntityKey entityKey = associationKey.getEntityKey();
		Node entityNode = entityQueries.get( entityKey.getMetadata() ).findEntity( executionEngine, entityKey.getColumnValues() );
		GraphLogger.log( "Found owner node: %1$s", entityNode );
		if ( entityNode == null ) {
			return null;
		}
		return new Association(
				new Neo4jAssociationSnapshot(
						entityNode,
						associationKey,
						associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata(),
						associationContext.getAssociationTypeContext().getRoleOnMainSide()
				)
		);
	}

	@Override
	public Association createAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		return new Association();
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
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
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return neo4jSequenceGenerator.nextValue( request.getKey(), request.getIncrement(), request.getInitialValue() );
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

		associationQueries.get( key.getMetadata() ).removeAssociation( executionEngine, key );
	}

	private void applyAssociationOperation(Association association, AssociationKey key, AssociationOperation operation, AssociationContext associationContext) {
		switch ( operation.getType() ) {
		case CLEAR:
			removeAssociation( key, associationContext );
			break;
		case PUT:
			putAssociationOperation( association, key, operation, associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata() );
			break;
		case PUT_NULL:
		case REMOVE:
			removeAssociationOperation( association, key, operation, associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata() );
			break;
		}
	}

	private void putAssociationOperation(Association association, AssociationKey associationKey, AssociationOperation action, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		Relationship relationship = associationQueries.get( associationKey.getMetadata() ).findRelationship( executionEngine, associationKey, action.getKey() );

		if (relationship != null) {
			for ( String relationshipProperty : associationKey.getMetadata().getRowKeyIndexColumnNames() ) {
				relationship.setProperty( relationshipProperty, action.getValue().get( relationshipProperty ) );

			}
			GraphLogger.log( "Updated relationship: %1$s", relationship );
		}
		else {
			relationship = createRelationship( associationKey, action.getValue(), associatedEntityKeyMetadata );
			GraphLogger.log( "Created relationship: %1$s", relationship );
		}
	}

	private void removeAssociationOperation(Association association, AssociationKey associationKey, AssociationOperation action, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		associationQueries.get( associationKey.getMetadata() ).removeAssociationRow( executionEngine, associationKey, action.getKey() );
	}

	private void applyTupleOperations(EntityKey entityKey, Tuple tuple, Node node, Set<TupleOperation> operations, TupleContext tupleContext) {
		Set<String> processedAssociationRoles = new HashSet<String>();

		for ( TupleOperation operation : operations ) {
			applyOperation( entityKey, tuple, node, operation, tupleContext, processedAssociationRoles );
		}
	}

	private void applyOperation(EntityKey entityKey, Tuple tuple, Node node, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		switch ( operation.getType() ) {
		case PUT:
			putTupleOperation( entityKey, tuple, node, operation, tupleContext, processedAssociationRoles );
			break;
		case PUT_NULL:
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

				Iterator<Relationship> relationships = node.getRelationships( withName( associationRole ) ).iterator();

				if ( relationships.hasNext() ) {
					relationships.next().delete();
				}
			}
		}
	}

	private void putTupleOperation(EntityKey entityKey, Tuple tuple, Node node, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		if ( !tupleContext.isPartOfAssociation( operation.getColumn() ) ) {
			try {
				node.setProperty( operation.getColumn(), operation.getValue() );
			}
			catch (ConstraintViolationException e) {
				throw log.constraintViolation( entityKey, operation, e );
			}
		}
		// the column represents a to-one association, map it as relationship
		else {
			String associationRole = tupleContext.getRole( operation.getColumn() );

			if ( !processedAssociationRoles.contains( associationRole ) ) {
				processedAssociationRoles.add( associationRole );

				EntityKey targetKey = getEntityKey( tuple, tupleContext.getAssociatedEntityKeyMetadata( operation.getColumn() ) );

				// delete the previous relationship if there is one; for a to-one association, the relationship won't have any
				// properties, so the type is uniquely identifying it
				Iterator<Relationship> relationships = node.getRelationships( withName( associationRole ) ).iterator();
				if ( relationships.hasNext() ) {
					relationships.next().delete();
				}

				// create a new relationship
				Node targetNode = entityQueries.get( targetKey.getMetadata() ).findEntity( executionEngine, targetKey.getColumnValues() );
				node.createRelationshipTo( targetNode, withName( associationRole ) );
			}
		}
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			ResourceIterator<Node> queryNodes = entityQueries.get( entityKeyMetadata ).findEntities( executionEngine );
			try {
				while ( queryNodes.hasNext() ) {
					Node next = queryNodes.next();
					Tuple tuple = new Tuple( new Neo4jTupleSnapshot( next ) );
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
		ExecutionResult result = executionEngine.execute( nativeQuery, parameters );

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

	/**
	 * Returns the key of the entity targeted by the represented association, retrieved from the given tuple.
	 *
	 * @param tuple the tuple from which to retrieve the referenced entity key
	 * @return the key of the entity targeted by the represented association
	 */
	private EntityKey getEntityKey(Tuple tuple, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		Object[] columnValues = new Object[ associatedEntityKeyMetadata.getAssociationKeyColumns().length];
		int i = 0;

		for ( String associationKeyColumn : associatedEntityKeyMetadata.getAssociationKeyColumns() ) {
			columnValues[i] = tuple.get( associationKeyColumn );
			i++;
		}

		return new EntityKey( associatedEntityKeyMetadata.getEntityKeyMetadata(), columnValues );
	}
}
