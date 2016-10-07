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
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl.RemoteNeo4jAssociationPropertiesRow;
import org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl.RemoteNeo4jAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.common.util.impl.RemoteNeo4jHelper;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.HttpNeo4jAssociatedNodesHelper;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.HttpNeo4jAssociationQueries;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.HttpNeo4jEntityQueries;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.HttpNeo4jMapsTupleIterator;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.HttpNeo4jNodesTupleIterator;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.HttpNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.HttpNeo4jTupleAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.HttpNeo4jTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.HttpNeo4jTypeConverter;
import org.hibernate.ogm.datastore.neo4j.remote.http.dialect.impl.NodeWithEmbeddedNodes;
import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.ErrorResponse;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph.Node;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Graph.Relationship;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Row;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statement;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementResult;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementsResponse;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.AssociationOperation;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
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
public class HttpNeo4jDialect extends BaseNeo4jDialect implements RemoteNeo4jDialect {

	private static final Log log = LoggerFactory.getLogger();

	private final HttpNeo4jClient client;

	private final HttpNeo4jSequenceGenerator sequenceGenerator;

	private Map<EntityKeyMetadata, HttpNeo4jEntityQueries> entityQueries;

	private Map<AssociationKeyMetadata, HttpNeo4jAssociationQueries> associationQueries;

	public HttpNeo4jDialect(HttpNeo4jDatastoreProvider provider) {
		super( HttpNeo4jTypeConverter.INSTANCE );
		this.client = provider.getClient();
		this.sequenceGenerator = provider.getSequenceGenerator();
	}

	@Override
	public void sessionFactoryCreated(SessionFactoryImplementor sessionFactoryImplementor) {
		this.associationQueries = Collections.unmodifiableMap( initializeAssociationQueries( sessionFactoryImplementor ) );
		this.entityQueries = Collections.unmodifiableMap( initializeEntityQueries( sessionFactoryImplementor, associationQueries ) );
	}

	private Map<EntityKeyMetadata, HttpNeo4jEntityQueries> initializeEntityQueries(SessionFactoryImplementor sessionFactoryImplementor,
			Map<AssociationKeyMetadata, HttpNeo4jAssociationQueries> associationQueries) {
		Map<EntityKeyMetadata, HttpNeo4jEntityQueries> entityQueries = initializeEntityQueries( sessionFactoryImplementor );
		for ( AssociationKeyMetadata associationKeyMetadata : associationQueries.keySet() ) {
			EntityKeyMetadata entityKeyMetadata = associationKeyMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata();
			if ( !entityQueries.containsKey( entityKeyMetadata ) ) {
				// Embeddables metadata
				entityQueries.put( entityKeyMetadata, new HttpNeo4jEntityQueries( entityKeyMetadata, null ) );
			}
		}
		return entityQueries;
	}

	private Map<EntityKeyMetadata, HttpNeo4jEntityQueries> initializeEntityQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<EntityKeyMetadata, HttpNeo4jEntityQueries> queryMap = new HashMap<EntityKeyMetadata, HttpNeo4jEntityQueries>();
		Collection<EntityPersister> entityPersisters = sessionFactoryImplementor.getEntityPersisters().values();
		for ( EntityPersister entityPersister : entityPersisters ) {
			if (entityPersister instanceof OgmEntityPersister ) {
				OgmEntityPersister ogmEntityPersister = (OgmEntityPersister) entityPersister;
				queryMap.put( ogmEntityPersister.getEntityKeyMetadata(), new HttpNeo4jEntityQueries( ogmEntityPersister.getEntityKeyMetadata(),
						ogmEntityPersister.getTupleTypeContext() ) );
			}
		}
		return queryMap;
	}

	private Map<AssociationKeyMetadata, HttpNeo4jAssociationQueries> initializeAssociationQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<AssociationKeyMetadata, HttpNeo4jAssociationQueries> queryMap = new HashMap<AssociationKeyMetadata, HttpNeo4jAssociationQueries>();
		Collection<CollectionPersister> collectionPersisters = sessionFactoryImplementor.getCollectionPersisters().values();
		for ( CollectionPersister collectionPersister : collectionPersisters ) {
			if ( collectionPersister instanceof OgmCollectionPersister ) {
				OgmCollectionPersister ogmCollectionPersister = (OgmCollectionPersister) collectionPersister;
				EntityKeyMetadata ownerEntityKeyMetadata = ( (OgmEntityPersister) ( ogmCollectionPersister.getOwnerEntityPersister() ) ).getEntityKeyMetadata();
				AssociationKeyMetadata associationKeyMetadata = ogmCollectionPersister.getAssociationKeyMetadata();
				queryMap.put( associationKeyMetadata, new HttpNeo4jAssociationQueries( ownerEntityKeyMetadata, associationKeyMetadata ) );
			}
		}
		return queryMap;
	}

	@Override
	public Tuple getTuple(EntityKey key, OperationContext operationContext) {
		HttpNeo4jEntityQueries queries = entityQueries.get( key.getMetadata() );
		Long txId = transactionId( operationContext.getTransactionContext() );
		NodeWithEmbeddedNodes owner = queries.findEntity( client, txId, key.getColumnValues() );
		if ( owner == null ) {
			return null;
		}

		Map<String, Node> toOneEntities = HttpNeo4jAssociatedNodesHelper.findAssociatedNodes( client, txId, owner, key.getMetadata(),
				operationContext.getTupleTypeContext(), queries );

		return new Tuple(
				new HttpNeo4jTupleSnapshot(
						owner,
						key.getMetadata(),
						toOneEntities,
						operationContext.getTupleTypeContext() ),
				SnapshotType.UPDATE );
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
		HttpNeo4jEntityQueries queries = entityQueries.get( metadata );
		ClosableIterator<NodeWithEmbeddedNodes> nodes = queries.findEntities( client, keys, txId );
		try {
			return tuplesResult( keys, tupleContext, nodes, txId, queries );
		}
		finally {
			nodes.close();
		}
	}

	/*
	 * This method assumes that the nodes might not be in the same order as the keys and some keys might not have a
	 * matching result in the db.
	 */
	private List<Tuple> tuplesResult(EntityKey[] keys, TupleContext tupleContext, ClosableIterator<NodeWithEmbeddedNodes> nodes, Long txId, HttpNeo4jEntityQueries queries) {
		// The list is initialized with null because some keys might not have a corresponding node
		Tuple[] tuples = new Tuple[keys.length];
		while ( nodes.hasNext() ) {
			NodeWithEmbeddedNodes node = nodes.next();
			for ( int i = 0; i < keys.length; i++ ) {
				String[] keyNames = keys[i].getColumnNames();
				Object[] keyValues = keys[i].getColumnValues();
				Map<String, Object> nodeProperties = node.getOwner().getProperties();
				if ( RemoteNeo4jHelper.matches( nodeProperties, keyNames, keyValues ) ) {
					EntityKeyMetadata metadata = keys[i].getMetadata();
					Map<String, Node> toOneEntities = HttpNeo4jAssociatedNodesHelper.findAssociatedNodes( client, txId, node, metadata,
							tupleContext.getTupleTypeContext(), queries );
					tuples[i] = new Tuple(
							new HttpNeo4jTupleSnapshot(
									node,
									metadata,
									toOneEntities,
									tupleContext.getTupleTypeContext() ),
							SnapshotType.UPDATE );
					// We assume there are no duplicated keys
					break;
				}
			}
		}
		return Arrays.asList( tuples );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
		Tuple tuple = tuplePointer.getTuple();
		// insert
		final Map<String, EntityKey> toOneAssociations = new HashMap<>();
		Statements statements = new Statements();
		Map<String, Object> properties = new HashMap<>();
		applyTupleOperations( key, tuple, properties, toOneAssociations, statements, tuple.getOperations(), tupleContext, tupleContext.getTransactionContext() );
		if ( SnapshotType.INSERT.equals( tuple.getSnapshotType() ) ) {
			Statement statement = entityQueries.get( key.getMetadata() ).getCreateEntityWithPropertiesQueryStatement( key.getColumnValues(), properties );
			statements.getStatements().add( 0, statement );
		}
		else {
			updateTuple( key, statements, properties );
		}
		saveToOneAssociations( statements, key, toOneAssociations );
		Long txId = transactionId( tupleContext.getTransactionContext() );
		StatementsResponse readEntity = client.executeQueriesInOpenTransaction( txId, statements );
		validate( readEntity, key );
		tuple.setSnapshotType( SnapshotType.UPDATE );
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

	private void validate(StatementsResponse readEntity, EntityKey key) {
		if (!readEntity.getErrors().isEmpty() ) {
			ErrorResponse errorResponse = readEntity.getErrors().get( 0 );
			switch ( errorResponse.getCode() ) {
				case BaseNeo4jDialect.CONSTRAINT_VIOLATION_CODE:
					throw extractException( key, errorResponse );
				default:
					throw new HibernateException( String.valueOf( errorResponse ) );
			}
		}
	}

	private HibernateException extractException(EntityKey key, ErrorResponse errorResponse) {
		if ( errorResponse.getMessage().matches( ".*Node \\d+ already exists with label.*" ) ) {
			// This is the exception we expect for this kind of error by the CompensationAPI and some unit tests
			return new TupleAlreadyExistsException( key, errorResponse.getMessage() );
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
		entityQueries.get( key.getMetadata() ).removeEntity( client, txId, key.getColumnValues() );
	}

	/**
	 * When dealing with some scenarios like, for example, a bidirectional association, OGM calls this method twice:
	 * <p>
	 * the first time with the information related to the owner of the association and the {@link RowKey},
	 * the second time using the same {@link RowKey} but with the {@link AssociationKey} referring to the other side of the association.
	 * @param associatedEntityKeyMetadata
	 * @param action
	 */
	private void putAssociationOperation(AssociationKey associationKey, AssociationOperation action, AssociationContext associationContext) {
		switch ( associationKey.getMetadata().getAssociationKind() ) {
			case EMBEDDED_COLLECTION:
				createRelationshipWithEmbeddedNode( associationKey, associationContext, action );
				break;
			case ASSOCIATION:
				findOrCreateRelationshipWithEntityNode( associationKey, associationContext, action );
				break;
			default:
				throw new AssertionFailure( "Unrecognized associationKind: " + associationKey.getMetadata().getAssociationKind() );
		}
	}

	private void createRelationshipWithEmbeddedNode(AssociationKey associationKey, AssociationContext associationContext, AssociationOperation action) {
		AssociatedEntityKeyMetadata associatedEntityKeyMetadata = associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata();
		Long txId = transactionId( associationContext.getTransactionContext() );
		Tuple associationRow = action.getValue();
		EntityKey embeddedKey = getEntityKey( associationRow, associatedEntityKeyMetadata  );
		Object[] relationshipProperties = relationshipProperties( associationKey, action );

		associationQueries.get( associationKey.getMetadata() )
				.createRelationshipForEmbeddedAssociation( client, txId, associationKey, embeddedKey, relationshipProperties );
	}

	private Relationship findOrCreateRelationshipWithEntityNode(AssociationKey associationKey, AssociationContext associationContext, AssociationOperation action) {
		Tuple associationRow = action.getValue();
		EntityKey ownerKey = associationKey.getEntityKey();
		AssociatedEntityKeyMetadata associatedEntityKeyMetadata = associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata();
		EntityKey targetKey = getEntityKey( associationRow, associatedEntityKeyMetadata  );
		Object[] relationshipProperties = relationshipProperties( associationKey, associationRow );
		Long txId = transactionId( associationContext.getTransactionContext() );

		return associationQueries.get( associationKey.getMetadata() )
			.createRelationship( client, txId, ownerKey.getColumnValues(), targetKey.getColumnValues(), relationshipProperties );
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
		NodeWithEmbeddedNodes node = entityQueries.get( entityKey.getMetadata() ).findEntity( client, transactionId, entityKey.getColumnValues() );
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
				.findAssociation( client, txId, entityKey.getColumnValues(), relationshipType );
		while ( relationships.hasNext() ) {
			RemoteNeo4jAssociationPropertiesRow row = relationships.next();
			AssociatedEntityKeyMetadata associatedEntityKeyMetadata = associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata();
			HttpNeo4jTupleAssociationSnapshot snapshot = new HttpNeo4jTupleAssociationSnapshot( client,
					associationQueries.get( associationKey.getMetadata() ), row, associationKey, associatedEntityKeyMetadata );
			RowKey rowKey = convert( associationKey, snapshot );
			tuples.put( rowKey, new Tuple( snapshot, SnapshotType.UPDATE ) );
		}
		return tuples;
	}

	private RowKey convert(AssociationKey associationKey, HttpNeo4jTupleAssociationSnapshot snapshot) {
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
		associationQueries.get( key.getMetadata() ).removeAssociation( client, txId, key );
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
		associationQueries.get( associationKey.getMetadata() ).removeAssociationRow( client, txId, associationKey, action.getKey() );
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
				entityQueries.get( entityKey.getMetadata() ).removeToOneAssociation( client, txId, entityKey.getColumnValues(), associationRole );
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
		// Long txId = transactionId( tupleContext.getTransactionContext() );
		Long txId = null;
		HttpNeo4jEntityQueries queries = entityQueries.get( entityKeyMetadata );
		ClosableIterator<NodeWithEmbeddedNodes> queryNodes = entityQueries.get( entityKeyMetadata ).findEntitiesWithEmbedded( client, txId );
		while ( queryNodes.hasNext() ) {
			NodeWithEmbeddedNodes next = queryNodes.next();
			Map<String, Node> associatedEntities = HttpNeo4jAssociatedNodesHelper.findAssociatedNodes( client, txId, next, entityKeyMetadata, tupleTypeContext,
					queries );
			Tuple tuple = new Tuple( new HttpNeo4jTupleSnapshot( next, entityKeyMetadata, associatedEntities, tupleTypeContext ), SnapshotType.UPDATE );
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
			response = client.executeQueriesInOpenTransaction( txId, statements );
			EntityKeyMetadata entityKeyMetadata = backendQuery.getSingleEntityMetadataInformationOrNull().getEntityKeyMetadata();
			HttpNeo4jEntityQueries queries = entityQueries.get( entityKeyMetadata );
			List<StatementResult> results = response.getResults();
			List<Row> rows = results.get( 0 ).getData();
			EntityKey[] keys = new EntityKey[ rows.size() ];
			for ( int i = 0; i < rows.size(); i++ ) {
				Node node = rows.get( i ).getGraph().getNodes().get( 0 );
				Object[] values = columnValues( node, entityKeyMetadata );
				keys[i] = new EntityKey( entityKeyMetadata, values );
			}
			ClosableIterator<NodeWithEmbeddedNodes> entities = entityQueries.get( entityKeyMetadata ).findEntities( client, keys, txId );
			return new HttpNeo4jNodesTupleIterator( client, txId, queries, response, entityKeyMetadata, tupleContext.getTupleTypeContext(), entities );
		}
		else {
			statement.setResultDataContents( Arrays.asList( Statement.AS_ROW ) );
			response = client.executeQueriesInOpenTransaction( txId, statements );
			return new HttpNeo4jMapsTupleIterator( response.getResults().get( 0 ) );
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
