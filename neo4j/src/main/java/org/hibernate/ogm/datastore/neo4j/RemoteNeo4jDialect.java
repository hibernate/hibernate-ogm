/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.impl.EmptyTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.NodeWithEmbeddedNodes;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteNeo4jAssociationPropertiesRow;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteNeo4jAssociationQueries;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteNeo4jAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteNeo4jEntityQueries;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteNeo4jMapsTupleIterator;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteNeo4jNodesTupleIterator;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteNeo4jTupleAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteNeo4jTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteNeo4jTypeConverter;
import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.ErrorResponse;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Graph.Node;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Graph.Relationship;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Row;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statement;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementResult;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
import org.hibernate.ogm.datastore.neo4j.remote.util.impl.RemoteNeo4jHelper;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
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
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

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
public class RemoteNeo4jDialect extends BaseNeo4jDialect {

	private static final Log log = LoggerFactory.getLogger();

	private final RemoteNeo4jClient dataBase;

	private final RemoteNeo4jSequenceGenerator sequenceGenerator;

	private Map<EntityKeyMetadata, RemoteNeo4jEntityQueries> entityQueries;

	private Map<AssociationKeyMetadata, RemoteNeo4jAssociationQueries> associationQueries;

	public RemoteNeo4jDialect(RemoteNeo4jDatastoreProvider provider) {
		super( RemoteNeo4jTypeConverter.INSTANCE );
		this.dataBase = provider.getDatabase();
		this.sequenceGenerator = provider.getSequenceGenerator();
	}

	@Override
	public void sessionFactoryCreated(SessionFactoryImplementor sessionFactoryImplementor) {
		this.associationQueries = Collections.unmodifiableMap( initializeAssociationQueries( sessionFactoryImplementor ) );
		this.entityQueries = Collections.unmodifiableMap( initializeEntityQueries( sessionFactoryImplementor, associationQueries ) );
	}

	private Map<EntityKeyMetadata, RemoteNeo4jEntityQueries> initializeEntityQueries(SessionFactoryImplementor sessionFactoryImplementor,
			Map<AssociationKeyMetadata, RemoteNeo4jAssociationQueries> associationQueries) {
		Map<EntityKeyMetadata, RemoteNeo4jEntityQueries> entityQueries = initializeEntityQueries( sessionFactoryImplementor );
		for ( AssociationKeyMetadata associationKeyMetadata : associationQueries.keySet() ) {
			EntityKeyMetadata entityKeyMetadata = associationKeyMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata();
			if ( !entityQueries.containsKey( entityKeyMetadata ) ) {
				// Embeddables metadata
				entityQueries.put( entityKeyMetadata, new RemoteNeo4jEntityQueries( entityKeyMetadata, null ) );
			}
		}
		return entityQueries;
	}

	private Map<EntityKeyMetadata, RemoteNeo4jEntityQueries> initializeEntityQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<EntityKeyMetadata, RemoteNeo4jEntityQueries> queryMap = new HashMap<EntityKeyMetadata, RemoteNeo4jEntityQueries>();
		Collection<EntityPersister> entityPersisters = sessionFactoryImplementor.getEntityPersisters().values();
		for ( EntityPersister entityPersister : entityPersisters ) {
			if (entityPersister instanceof OgmEntityPersister ) {
				OgmEntityPersister ogmEntityPersister = (OgmEntityPersister) entityPersister;
				TupleTypeContext tupleTypeContext = ogmEntityPersister.getTupleTypeContext();
				queryMap.put( ogmEntityPersister.getEntityKeyMetadata(), new RemoteNeo4jEntityQueries( ogmEntityPersister.getEntityKeyMetadata(), tupleTypeContext ) );
			}
		}
		return queryMap;
	}

	private Map<AssociationKeyMetadata, RemoteNeo4jAssociationQueries> initializeAssociationQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<AssociationKeyMetadata, RemoteNeo4jAssociationQueries> queryMap = new HashMap<AssociationKeyMetadata, RemoteNeo4jAssociationQueries>();
		Collection<CollectionPersister> collectionPersisters = sessionFactoryImplementor.getCollectionPersisters().values();
		for ( CollectionPersister collectionPersister : collectionPersisters ) {
			if ( collectionPersister instanceof OgmCollectionPersister ) {
				OgmCollectionPersister ogmCollectionPersister = (OgmCollectionPersister) collectionPersister;
				EntityKeyMetadata ownerEntityKeyMetadata = ( (OgmEntityPersister) ( ogmCollectionPersister.getOwnerEntityPersister() ) ).getEntityKeyMetadata();
				AssociationKeyMetadata associationKeyMetadata = ogmCollectionPersister.getAssociationKeyMetadata();
				queryMap.put( associationKeyMetadata, new RemoteNeo4jAssociationQueries( ownerEntityKeyMetadata, associationKeyMetadata ) );
			}
		}
		return queryMap;
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext context) {
		RemoteNeo4jEntityQueries queries = entityQueries.get( key.getMetadata() );
		Long txId = transactionId( context.getTransactionContext() );
		NodeWithEmbeddedNodes node = queries.findEntity( dataBase, txId, key.getColumnValues() );
		if ( node == null ) {
			return null;
		}

		return new Tuple(
				new RemoteNeo4jTupleSnapshot(
						dataBase,
						txId,
						queries,
						node,
						context.getTupleTypeContext().getAllAssociatedEntityKeyMetadata(),
						context.getTupleTypeContext().getAllRoles(),
						key.getMetadata()
						)
				);
	}

	@Override
	public List<Tuple> getTuples(EntityKey[] keys, TupleContext tupleContext) {
		if ( keys.length == 0 ) {
			return Collections.emptyList();
		}

		Long txId = transactionId( tupleContext.getTransactionContext() );
		// We only supports one metadata for now
		EntityKeyMetadata metadata = keys[0].getMetadata();
		// The result returned by the query might not be in the same order as the keys.
		ClosableIterator<NodeWithEmbeddedNodes> nodes = entityQueries.get( metadata ).findEntities( dataBase, keys, txId );
		try {
			return tuplesResult( keys, tupleContext, nodes, txId );
		}
		finally {
			nodes.close();
		}
	}

	/*
	 * This method assumes that the nodes might not be in the same order as the keys and some keys might not have a
	 * matching result in the db.
	 */
	private List<Tuple> tuplesResult(EntityKey[] keys, TupleContext tupleContext, ClosableIterator<NodeWithEmbeddedNodes> nodes, Long txId) {
		// The list is initialized with null because some keys might not have a corresponding node
		Tuple[] tuples = new Tuple[keys.length];
		while ( nodes.hasNext() ) {
			NodeWithEmbeddedNodes node = nodes.next();
			for ( int i = 0; i < keys.length; i++ ) {
				if ( RemoteNeo4jHelper.matches( node.getOwner(), keys[i].getColumnNames(), keys[i].getColumnValues() ) ) {
					EntityKeyMetadata metadata = keys[i].getMetadata();
					tuples[i] = new Tuple(
							new RemoteNeo4jTupleSnapshot(
									dataBase,
									txId,
									entityQueries.get( metadata ),
									node,
									tupleContext.getTupleTypeContext().getAllAssociatedEntityKeyMetadata(),
									tupleContext.getTupleTypeContext().getAllRoles(),
									metadata
									)
							);
					// We assume there are no duplicated keys
					break;
				}
			}
		}
		return Arrays.asList( tuples );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		// insert
		final Map<String, EntityKey> toOneAssociations = new HashMap<>();
		Statements statements = new Statements();
		Map<String, Object> properties = new HashMap<>();
		applyTupleOperations( key, tuple, properties, toOneAssociations, statements, tuple.getOperations(), tupleContext, tupleContext.getTransactionContext() );
		if ( tuple.getSnapshot() instanceof EmptyTupleSnapshot ) {
			Statement statement = entityQueries.get( key.getMetadata() ).getCreateEntityWithPropertiesQueryStatement( key.getColumnValues(), properties );
			statements.getStatements().add( 0, statement );
		}
		else {
			updateTuple( key, statements, properties );
		}
		saveToOneAssociations( statements, key, toOneAssociations );
		Long txId = transactionId( tupleContext.getTransactionContext() );
		StatementsResponse readEntity = dataBase.executeQueriesInOpenTransaction( txId, statements );
		validate( readEntity, key, tuple );
	}

	private Long transactionId(TransactionContext context) {
		return (Long) context.getTransactionId();
	}

	private void updateTuple(EntityKey key, Statements statements, Map<String, Object> properties) {
		if ( !properties.isEmpty() ) {
			Statement statement = entityQueries.get( key.getMetadata() ).getUpdateEntityPropertiesStatement( key.getColumnValues(), properties );
			statements.addStatement( statement );
		}
	}

	private void validate(StatementsResponse readEntity, EntityKey key, Tuple tuple) {
		if (!readEntity.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = readEntity.getErrors().get( 0 );
			switch ( errorResponse.getCode() ) {
				case BaseNeo4jDialect.CONSTRAINT_VIOLATION_CODE:
					throw extractException( key, tuple, errorResponse );
				default:
					throw new HibernateException( String.valueOf( errorResponse ) );
			}
		}
	}

	private HibernateException extractException(EntityKey key, Tuple tuple, ErrorResponse errorResponse) {
		if ( errorResponse.getMessage().matches( ".*Node \\d+ already exists with label.*" ) ) {
			// This is the exception we expect for this kind of error by the CompensationAPI and some unit tests
			return new TupleAlreadyExistsException( key.getMetadata(), tuple, errorResponse.getMessage() );
		}
		else {
			return log.constraintViolation( key, errorResponse.getMessage(), null );
		}
	}

	private void saveToOneAssociations(Statements statements, EntityKey key, final Map<String, EntityKey> toOneAssociations) {
		for ( Map.Entry<String, EntityKey> entry : toOneAssociations.entrySet() ) {
			Statement statement = entityQueries.get( key.getMetadata() ).getUpdateOneToOneAssociationStatement( entry.getKey(), key.getColumnValues(), entry.getValue().getColumnValues() );
			statements.addStatement( statement );
		}
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		Long txId = transactionId( tupleContext.getTransactionContext() );
		entityQueries.get( key.getMetadata() ).removeEntity( dataBase, txId, key.getColumnValues() );
	}

	/**
	 * When dealing with some scenarios like, for example, a bidirectional association, OGM calls this method twice:
	 * <p>
	 * the first time with the information related to the owner of the association and the {@link RowKey},
	 * the second time using the same {@link RowKey} but with the {@link AssociationKey} referring to the other side of the association.
	 * @param associatedEntityKeyMetadata
	 * @param action
	 */
	private Relationship putAssociationOperation(AssociationKey associationKey, AssociationOperation action, AssociationContext associationContext) {
		switch ( associationKey.getMetadata().getAssociationKind() ) {
			case EMBEDDED_COLLECTION:
				return createRelationshipWithEmbeddedNode( associationKey, associationContext, action );
			case ASSOCIATION:
				return findOrCreateRelationshipWithEntityNode( associationKey, associationContext, action );
			default:
				throw new AssertionFailure( "Unrecognized associationKind: " + associationKey.getMetadata().getAssociationKind() );
		}
	}

	private Relationship createRelationshipWithEmbeddedNode(AssociationKey associationKey, AssociationContext associationContext, AssociationOperation action) {
		AssociatedEntityKeyMetadata associatedEntityKeyMetadata = associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata();
		Long txId = transactionId( associationContext.getTransactionContext() );
		Tuple associationRow = action.getValue();
		EntityKey embeddedKey = getEntityKey( associationRow, associatedEntityKeyMetadata  );
		Object[] relationshipProperties = relationshipProperties( associationKey, action );

		Relationship relationship = associationQueries.get( associationKey.getMetadata() )
				.createRelationshipForEmbeddedAssociation( dataBase, txId, associationKey, embeddedKey, relationshipProperties );
		return relationship;
	}

	private Relationship findOrCreateRelationshipWithEntityNode(AssociationKey associationKey, AssociationContext associationContext, AssociationOperation action) {
		Tuple associationRow = action.getValue();
		EntityKey ownerKey = associationKey.getEntityKey();
		AssociatedEntityKeyMetadata associatedEntityKeyMetadata = associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata();
		EntityKey targetKey = getEntityKey( associationRow, associatedEntityKeyMetadata  );
		Object[] relationshipProperties = relationshipProperties( associationKey, associationRow );
		Long txId = transactionId( associationContext.getTransactionContext() );

		return associationQueries.get( associationKey.getMetadata() )
			.createRelationship( dataBase, txId, ownerKey.getColumnValues(), targetKey.getColumnValues(), relationshipProperties );
	}

	private Object[] relationshipProperties(AssociationKey associationKey, Tuple associationRow) {
		String[] indexColumns = associationKey.getMetadata().getRowKeyIndexColumnNames();
		Object[] properties = new Object[indexColumns.length];
		for ( int i = 0; i < indexColumns.length; i++ ) {
			String propertyName = indexColumns[i];
			properties[i] = associationRow.get( propertyName );
		}
		return properties;
	}

	@Override
	public Association getAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		EntityKey entityKey = associationKey.getEntityKey();
		Long transactionId = transactionId( associationContext.getTransactionContext() );
		NodeWithEmbeddedNodes node = entityQueries.get( entityKey.getMetadata() ).findEntity( dataBase, transactionId, entityKey.getColumnValues() );
		if ( node == null ) {
			return null;
		}

		Map<RowKey, Tuple> tuples = createAssociationMap( associationKey, associationContext, entityKey, associationContext.getTransactionContext() );
		return new Association( new RemoteNeo4jAssociationSnapshot( tuples ) );
	}

	private Map<RowKey, Tuple> createAssociationMap(AssociationKey associationKey, AssociationContext associationContext, EntityKey entityKey, TransactionContext transactionContext) {
		String relationshipType = associationContext.getAssociationTypeContext().getRoleOnMainSide();
		Map<RowKey, Tuple> tuples = new HashMap<RowKey, Tuple>();

		Long txId = transactionId( transactionContext );
		ClosableIterator<RemoteNeo4jAssociationPropertiesRow> relationships = entityQueries.get( entityKey.getMetadata() )
				.findAssociation( dataBase, txId, entityKey.getColumnValues(), relationshipType );
		while ( relationships.hasNext() ) {
			RemoteNeo4jAssociationPropertiesRow row = relationships.next();
			AssociatedEntityKeyMetadata associatedEntityKeyMetadata = associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata();
			RemoteNeo4jTupleAssociationSnapshot snapshot = new RemoteNeo4jTupleAssociationSnapshot( dataBase,
					associationQueries.get( associationKey.getMetadata() ), row, associationKey, associatedEntityKeyMetadata );
			RowKey rowKey = convert( associationKey, snapshot );
			tuples.put( rowKey, new Tuple( snapshot ) );
		}
		return tuples;
	}

	private RowKey convert(AssociationKey associationKey, RemoteNeo4jTupleAssociationSnapshot snapshot) {
		String[] columnNames = associationKey.getMetadata().getRowKeyColumnNames();
		Object[] values = new Object[columnNames.length];

		for ( int i = 0; i < columnNames.length; i++ ) {
			values[i] = snapshot.get( columnNames[i] );
		}

		return new RowKey( columnNames, values );
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
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		// If this is the inverse side of a bi-directional association, we don't manage the relationship from this side
		if ( key.getMetadata().isInverse() ) {
			return;
		}

		Long txId = transactionId( associationContext.getTransactionContext() );
		associationQueries.get( key.getMetadata() ).removeAssociation( dataBase, txId, key );
	}

	private void applyAssociationOperation(Association association, AssociationKey key, AssociationOperation operation, AssociationContext associationContext) {
		switch ( operation.getType() ) {
		case CLEAR:
			removeAssociation( key, associationContext );
			break;
		case PUT:
			putAssociationOperation( key, operation, associationContext );
			break;
		case REMOVE:
			removeAssociationOperation( key, operation, associationContext );
			break;
		}
	}

	private Object[] relationshipProperties(AssociationKey associationKey, AssociationOperation action) {
		Object[] relationshipProperties = new Object[associationKey.getMetadata().getRowKeyIndexColumnNames().length];
		String[] indexColumns = associationKey.getMetadata().getRowKeyIndexColumnNames();
		for ( int i = 0; i < indexColumns.length; i++ ) {
			relationshipProperties[i] = action.getValue().get( indexColumns[i] );
		}
		return relationshipProperties;
	}

	private void removeAssociationOperation(AssociationKey associationKey, AssociationOperation action, AssociationContext associationContext) {
		Long txId = transactionId( associationContext.getTransactionContext() );
		associationQueries.get( associationKey.getMetadata() ).removeAssociationRow( dataBase, txId, associationKey, action.getKey() );
	}

	private void applyTupleOperations(EntityKey entityKey, Tuple tuple, Map<String, Object> node, Map<String, EntityKey> toOneAssociations, Statements statements, Set<TupleOperation> operations, TupleContext tupleContext, TransactionContext transactionContext) {
		Set<String> processedAssociationRoles = new HashSet<String>();

		for ( TupleOperation operation : operations ) {
			applyOperation( entityKey, tuple, node, toOneAssociations, statements, operation, tupleContext, transactionContext, processedAssociationRoles );
		}
	}

	private void applyOperation(EntityKey entityKey, Tuple tuple, Map<String, Object> node, Map<String, EntityKey> toOneAssociations, Statements statements, TupleOperation operation, TupleContext tupleContext, TransactionContext transactionContext, Set<String> processedAssociationRoles) {
		switch ( operation.getType() ) {
		case PUT:
			putTupleOperation( entityKey, tuple, node, toOneAssociations, statements, operation, tupleContext, processedAssociationRoles );
			break;
		case PUT_NULL:
		case REMOVE:
			removeTupleOperation( entityKey, node, operation, statements, tupleContext, transactionContext, processedAssociationRoles );
			break;
		}
	}

	private void removeTupleOperation(EntityKey entityKey, Map<String, Object> ownerNode, TupleOperation operation, Statements statements, TupleContext tupleContext, TransactionContext transactionContext, Set<String> processedAssociationRoles) {
		if ( !tupleContext.getTupleTypeContext().isPartOfAssociation( operation.getColumn() ) ) {
			if ( isPartOfRegularEmbedded( entityKey.getColumnNames(), operation.getColumn() ) ) {
				// Embedded node
				Statement statement = entityQueries.get( entityKey.getMetadata() ).removeEmbeddedColumnStatement( entityKey.getColumnValues(),
						operation.getColumn() );
				statements.addStatement( statement );
			}
			else {
				Statement statement = entityQueries.get( entityKey.getMetadata() ).removeColumnStatement( entityKey.getColumnValues(), operation.getColumn() );
				statements.addStatement( statement );
			}
		}
		else {
			String associationRole = tupleContext.getTupleTypeContext().getRole( operation.getColumn() );
			if ( !processedAssociationRoles.contains( associationRole ) ) {
				Long txId = transactionId( transactionContext );
				entityQueries.get( entityKey.getMetadata() ).removeToOneAssociation( dataBase, txId, entityKey.getColumnValues(), associationRole );
			}
		}
	}

	private void putTupleOperation(EntityKey entityKey, Tuple tuple, Map<String, Object> node, Map<String, EntityKey> toOneAssociations, Statements statements, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		if ( tupleContext.getTupleTypeContext().isPartOfAssociation( operation.getColumn() ) ) {
			// the column represents a to-one association, map it as relationship
			putOneToOneAssociation( entityKey, tuple, node, toOneAssociations, operation, tupleContext, processedAssociationRoles );
		}
		else if ( isPartOfRegularEmbedded( entityKey.getMetadata().getColumnNames(), operation.getColumn() ) ) {
			Statement statement = entityQueries.get( entityKey.getMetadata() ).updateEmbeddedColumnStatement( entityKey.getColumnValues(), operation.getColumn(), operation.getValue() );
			statements.addStatement( statement );
		}
		else {
			putProperty( entityKey, node, operation );
		}
	}

	private void putProperty(EntityKey entityKey, Map<String, Object> node, TupleOperation operation) {
		node.put( operation.getColumn(), operation.getValue() );
	}

	private void putOneToOneAssociation(EntityKey ownerKey, Tuple tuple, Map<String, Object> node, Map<String, EntityKey> toOneAssociations, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		String associationRole = tupleContext.getTupleTypeContext().getRole( operation.getColumn() );

		if ( !processedAssociationRoles.contains( associationRole ) ) {
			processedAssociationRoles.add( associationRole );

			EntityKey targetKey = getEntityKey( tuple, tupleContext.getTupleTypeContext().getAssociatedEntityKeyMetadata( operation.getColumn() ) );

			toOneAssociations.put( associationRole, targetKey );
		}
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
		// TODO OGM-1111 we don't have a transaction context here as we are not in a session yet.
		// This is now clear thanks to the new TupleTypeContext contract.
		//Long txId = transactionId( tupleContext.getTransactionContext() );
		Long txId = null;
		RemoteNeo4jEntityQueries queries = entityQueries.get( entityKeyMetadata );
		ClosableIterator<NodeWithEmbeddedNodes> queryNodes = entityQueries.get( entityKeyMetadata ).findEntitiesWithEmbedded( dataBase, txId );
		while ( queryNodes.hasNext() ) {
			NodeWithEmbeddedNodes next = queryNodes.next();
			Tuple tuple = new Tuple( new RemoteNeo4jTupleSnapshot( dataBase, txId, queries, next, entityKeyMetadata ) );
			consumer.consume( tuple );
		}
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<String> backendQuery, QueryParameters queryParameters, TupleContext tupleContext) {
		Map<String, Object> parameters = getParameters( queryParameters );
		String nativeQuery = buildNativeQuery( backendQuery, queryParameters );
		Statement statement = new Statement( nativeQuery, parameters );
		Statements statements = new Statements();
		statements.addStatement( statement );
		Long txId = transactionId( tupleContext.getTransactionContext() );
		StatementsResponse response = null;
		if ( backendQuery.getSingleEntityMetadataInformationOrNull() != null ) {
			response = dataBase.executeQueriesInOpenTransaction( txId, statements );
			EntityKeyMetadata entityKeyMetadata = backendQuery.getSingleEntityMetadataInformationOrNull().getEntityKeyMetadata();
			RemoteNeo4jEntityQueries queries = entityQueries.get( entityKeyMetadata );
			List<StatementResult> results = response.getResults();
			List<Row> rows = results.get( 0 ).getData();
			EntityKey[] keys = new EntityKey[ rows.size() ];
			for ( int i = 0; i < rows.size(); i++ ) {
				Node node = rows.get( i ).getGraph().getNodes().get( 0 );
				Object[] values = columnValues( node, entityKeyMetadata );
				keys[i] = new EntityKey( entityKeyMetadata, values );
			}
			ClosableIterator<NodeWithEmbeddedNodes> entities = entityQueries.get( entityKeyMetadata ).findEntities( dataBase, keys, txId );
			return new RemoteNeo4jNodesTupleIterator( dataBase, txId, queries, response, entityKeyMetadata, tupleContext, entities );
		}
		else {
			statement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
			response = dataBase.executeQueriesInOpenTransaction( txId, statements );
			return new RemoteNeo4jMapsTupleIterator( response );
		}
	}

	private Object[] columnValues(Node node, EntityKeyMetadata metadata) {
		Object[] values = new Object[metadata.getColumnNames().length];
		for ( int i = 0; i < metadata.getColumnNames().length; i++ ) {
			values[i] = node.getProperties().get( metadata.getColumnNames()[i] );
		}
		return values;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return sequenceGenerator.nextValue( request );
	}
}
