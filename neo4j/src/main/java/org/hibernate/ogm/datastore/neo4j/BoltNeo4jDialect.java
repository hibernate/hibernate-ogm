/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.BoltNeo4jAssociatedNodesHelper;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.BoltNeo4jAssociationQueries;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.BoltNeo4jEntityQueries;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.BoltNeo4jMapsTupleIterator;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.BoltNeo4jNodesTupleIterator;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.BoltNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.BoltNeo4jTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.BoltNeo4jTypeConverter;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.dialect.impl.NodeWithEmbeddedNodes;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jClient;
import org.hibernate.ogm.datastore.neo4j.remote.bolt.impl.BoltNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl.RemoteNeo4jAssociationPropertiesRow;
import org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl.RemoteNeo4jAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl.RemoteNeo4jTupleAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.remote.common.util.impl.RemoteNeo4jHelper;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
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
import org.hibernate.ogm.dialect.spi.TuplesSupplier;
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
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Statement;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.neo4j.driver.v1.summary.ResultSummary;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Relationship;

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
public class BoltNeo4jDialect extends BaseNeo4jDialect<BoltNeo4jEntityQueries, BoltNeo4jAssociationQueries> implements RemoteNeo4jDialect {

	public static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final BoltNeo4jSequenceGenerator sequenceGenerator;

	public BoltNeo4jDialect(BoltNeo4jDatastoreProvider provider) {
		super( BoltNeo4jTypeConverter.INSTANCE );
		this.sequenceGenerator = provider.getSequenceGenerator();
	}

	@Override
	protected BoltNeo4jAssociationQueries createNeo4jAssociationQueries(EntityKeyMetadata ownerEntityKeyMetadata, AssociationKeyMetadata associationKeyMetadata) {
		return new BoltNeo4jAssociationQueries( ownerEntityKeyMetadata, associationKeyMetadata );
	}

	@Override
	protected BoltNeo4jEntityQueries createNeo4jEntityQueries(EntityKeyMetadata entityKeyMetadata, TupleTypeContext tupleTypeContext) {
		return new BoltNeo4jEntityQueries( entityKeyMetadata, tupleTypeContext );
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<String> backendQuery, QueryParameters queryParameters, TupleContext tupleContext) {
		Map<String, Object> parameters = getParameters( queryParameters );
		String nativeQuery = buildNativeQuery( backendQuery, queryParameters );
		Statement statement = new Statement( nativeQuery, parameters );
		if ( backendQuery.getSingleEntityMetadataInformationOrNull() != null ) {
				EntityKeyMetadata entityKeyMetadata = backendQuery.getSingleEntityMetadataInformationOrNull().getEntityKeyMetadata();
				BoltNeo4jEntityQueries queries = getEntityQueries( entityKeyMetadata, tupleContext );
				Transaction transaction = transaction( tupleContext );
				StatementResult result = transaction.run( statement );
				validateNativeQuery( result );
				List<EntityKey> entityKeys = new ArrayList<>();
				while ( result.hasNext() ) {
					Record record = result.next();
					Map<String, Object> recordAsMap = record.get( 0 ).asMap();
					Object[] columnValues = columnValues( recordAsMap, entityKeyMetadata );
					entityKeys.add( new EntityKey( entityKeyMetadata, columnValues ) );
				}
				EntityKey[] keys = entityKeys.toArray( new EntityKey[entityKeys.size()] );
				ClosableIterator<NodeWithEmbeddedNodes> entities = getEntityQueries( entityKeyMetadata, tupleContext ).findEntities( keys, transaction );
				return new BoltNeo4jNodesTupleIterator( transaction, queries, entityKeyMetadata, tupleContext.getTupleTypeContext(), entities );
		}
		else {
			Transaction transaction = transaction( tupleContext );
			StatementResult statementResult = transaction.run( statement );
			validateNativeQuery( statementResult );
			return new BoltNeo4jMapsTupleIterator( statementResult );
		}
	}

	@Override
	public int executeBackendUpdateQuery(BackendQuery<String> query, QueryParameters queryParameters, TupleContext tupleContext) {
		Map<String, Object> parameters = getParameters( queryParameters );
		String nativeQuery = buildNativeQuery( query, queryParameters );
		Statement statement = new Statement( nativeQuery, parameters );
		Transaction transaction = transaction( tupleContext );
		StatementResult statementResult = transaction.run( statement );
		validateNativeQuery( statementResult );
		ResultSummary summary = statementResult.consume();
		return updatesCount( summary );
	}

	private void validateNativeQuery(StatementResult result) {
		try {
			result.hasNext();
		}
		catch (ClientException e) {
			throw log.nativeQueryException( e.code(), e.getMessage(), null );
		}

	}

	private int updatesCount(ResultSummary summary) {
		int updates = 0;
		if ( summary.counters().containsUpdates() ) {
			updates += summary.counters().constraintsAdded();
			updates += summary.counters().constraintsRemoved();
			updates += summary.counters().nodesCreated();
			updates += summary.counters().nodesDeleted();
			updates += summary.counters().relationshipsCreated();
			updates += summary.counters().relationshipsDeleted();
			updates += summary.counters().labelsAdded();
			updates += summary.counters().labelsRemoved();
			updates += summary.counters().indexesAdded();
			updates += summary.counters().indexesRemoved();
			updates += summary.counters().propertiesSet();
		}
		return updates;
	}

	private Transaction transaction(OperationContext operationContext) {
		return (Transaction) ( operationContext.getTransactionContext() ).getTransactionId();
	}

	private Transaction transaction(AssociationContext associationContext) {
		return (Transaction) ( associationContext.getTransactionContext() ).getTransactionId();
	}

	private Object[] columnValues(Map<String, Object> node, EntityKeyMetadata metadata) {
		Object[] values = new Object[metadata.getColumnNames().length];
		for ( int i = 0; i < metadata.getColumnNames().length; i++ ) {
			values[i] = node.get( metadata.getColumnNames()[i] );
		}
		return values;
	}

	@Override
	public Tuple getTuple(EntityKey key, OperationContext operationContext) {
		Transaction tx = transaction( operationContext );
		BoltNeo4jEntityQueries queries = getEntityQueries( key.getMetadata(), operationContext );
		NodeWithEmbeddedNodes owner = queries.findEntity( tx, key.getColumnValues() );
		if ( owner == null ) {
			return null;
		}

		Map<String, Node> toOneEntities = BoltNeo4jAssociatedNodesHelper.findAssociatedNodes( tx, owner, key.getMetadata(),
				operationContext.getTupleTypeContext(), queries );

		return new Tuple(
				new BoltNeo4jTupleSnapshot(
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

		// We only support one metadata for now
		EntityKeyMetadata metadata = keys[0].getMetadata();
		// The result returned by the query might not be in the same order as the keys.
		ClosableIterator<NodeWithEmbeddedNodes> nodes = getEntityQueries( metadata, tupleContext ).findEntities( keys, transaction( tupleContext ) );
		try {
			return tuplesResult( keys, tupleContext, nodes );
		}
		finally {
			nodes.close();
		}
	}

	/*
	 * This method assumes that the nodes might not be in the same order as the keys and some keys might not have a
	 * matching result in the db.
	 */
	private List<Tuple> tuplesResult(EntityKey[] keys, TupleContext tupleContext, ClosableIterator<NodeWithEmbeddedNodes> nodes) {
		// The list is initialized with null because some keys might not have a corresponding node
		Tuple[] tuples = new Tuple[keys.length];
		Transaction tx = transaction( tupleContext );
		while ( nodes.hasNext() ) {
			NodeWithEmbeddedNodes node = nodes.next();
			for ( int i = 0; i < keys.length; i++ ) {
				if ( RemoteNeo4jHelper.matches( node.getOwner().asMap(), keys[i].getColumnNames(), keys[i].getColumnValues() ) ) {
					EntityKeyMetadata metadata = keys[i].getMetadata();
					EntityKey key = keys[i];
					BoltNeo4jEntityQueries entityQueries = getEntityQueries( key.getMetadata(), tupleContext );
					Map<String, Node> toOneEntities = BoltNeo4jAssociatedNodesHelper.findAssociatedNodes( tx, node, metadata,
							tupleContext.getTupleTypeContext(), entityQueries );
					tuples[i] = new Tuple(
							new BoltNeo4jTupleSnapshot(
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
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) throws TupleAlreadyExistsException {
		Tuple tuple = tuplePointer.getTuple();
		final Map<String, EntityKey> toOneAssociations = new HashMap<>();
		Map<String, Object> properties = new HashMap<>();
		List<Statement> statements = new ArrayList<>();
		applyTupleOperations( key, tuple, properties, toOneAssociations, statements, tuple.getOperations(), tupleContext, tupleContext.getTransactionContext() );
		if ( SnapshotType.INSERT.equals( tuple.getSnapshotType() ) ) {
			// Insert new node
			Statement statement = getEntityQueries( key.getMetadata(), tupleContext ).getCreateEntityWithPropertiesQueryStatement( key.getColumnValues(), properties );
			statements.add( 0, statement );
		}
		else {
			updateTuple( key, statements, properties, tupleContext );
		}
		saveToOneAssociations( statements, key, toOneAssociations, tupleContext );
		try {
			runAll( transaction( tupleContext ), statements );
			tuple.setSnapshotType( SnapshotType.UPDATE );
		}
		catch (ClientException e) {
			switch ( e.code() ) {
				case BaseNeo4jDialect.CONSTRAINT_VIOLATION_CODE:
					throw extractException( key, e );
				default:
					throw new HibernateException( e.getMessage() );
			}
		}
	}

	private void runAll(Transaction tx, List<Statement> statements) {
		for ( Statement statement : statements ) {
			StatementResult result = tx.run( statement );
			validate( result );
		}
	}

	private void validate(StatementResult result) {
		result.hasNext();
	}

	private HibernateException extractException(EntityKey key, ClientException exception) {
		if ( TUPLE_ALREADY_EXISTS_EXCEPTION_PATTERN.matcher( exception.getMessage() ).matches() ) {
			// This is the exception we expect for this kind of error by the CompensationAPI and some unit tests
			return new TupleAlreadyExistsException( key, exception.getMessage() );
		}
		else {
			return log.constraintViolation( key, exception.getMessage(), null );
		}
	}

	private void updateTuple(EntityKey key, List<Statement> statements, Map<String, Object> properties, TupleContext tupleContext) {
		if ( !properties.isEmpty() ) {
			Statement statement = getEntityQueries( key.getMetadata(), tupleContext ).getUpdateEntityPropertiesStatement( key.getColumnValues(), properties );
			statements.add( statement );
		}
	}

	private void applyTupleOperations(EntityKey entityKey, Tuple tuple, Map<String, Object> node, Map<String, EntityKey> toOneAssociations, List<Statement> statements, Set<TupleOperation> operations, TupleContext tupleContext, TransactionContext transactionContext) {
		Set<String> processedAssociationRoles = new HashSet<String>();

		for ( TupleOperation operation : operations ) {
			applyOperation( entityKey, tuple, node, toOneAssociations, statements, operation, tupleContext, transactionContext, processedAssociationRoles );
		}
	}

	private void applyOperation(EntityKey entityKey, Tuple tuple, Map<String, Object> node, Map<String, EntityKey> toOneAssociations, List<Statement> statements, TupleOperation operation, TupleContext tupleContext, TransactionContext transactionContext, Set<String> processedAssociationRoles) {
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

	private void saveToOneAssociations(List<Statement> statements, EntityKey key, final Map<String, EntityKey> toOneAssociations, TupleContext tupleContext) {
		for ( Map.Entry<String, EntityKey> entry : toOneAssociations.entrySet() ) {
			Statement statement = getEntityQueries( key.getMetadata(), tupleContext ).getUpdateOneToOneAssociationStatement( entry.getKey(), key.getColumnValues(), entry.getValue().getColumnValues() );
			statements.add( statement );
		}
	}

	private void removeTupleOperation(EntityKey entityKey, Map<String, Object> ownerNode, TupleOperation operation, List<Statement> statements, TupleContext tupleContext, TransactionContext transactionContext, Set<String> processedAssociationRoles) {
		if ( !tupleContext.getTupleTypeContext().isPartOfAssociation( operation.getColumn() ) ) {
			if ( isPartOfRegularEmbedded( entityKey.getColumnNames(), operation.getColumn() ) ) {
				// Embedded node
				Statement statement = getEntityQueries( entityKey.getMetadata(), tupleContext ).removeEmbeddedColumnStatement( entityKey.getColumnValues(), operation.getColumn(), transaction( tupleContext ) );
				statements.add( statement );
			}
			else {
				Statement statement = getEntityQueries( entityKey.getMetadata(), tupleContext ).removeColumnStatement( entityKey.getColumnValues(), operation.getColumn(), transaction( tupleContext ) );
				statements.add( statement );
			}
		}
		else {
			String associationRole = tupleContext.getTupleTypeContext().getRole( operation.getColumn() );
			if ( !processedAssociationRoles.contains( associationRole ) ) {
				Transaction tx = (Transaction) transactionContext.getTransactionId();
				getEntityQueries( entityKey.getMetadata(), tupleContext.getTupleTypeContext() ).removeToOneAssociation( tx, entityKey.getColumnValues(), associationRole );
			}
		}
	}

	private void putTupleOperation(EntityKey entityKey, Tuple tuple, Map<String, Object> node, Map<String, EntityKey> toOneAssociations,
			List<Statement> statements, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		if ( tupleContext.getTupleTypeContext().isPartOfAssociation( operation.getColumn() ) ) {
			// the column represents a to-one association, map it as relationship
			putOneToOneAssociation( entityKey, tuple, node, toOneAssociations, operation, tupleContext, processedAssociationRoles );
		}
		else if ( isPartOfRegularEmbedded( entityKey.getMetadata().getColumnNames(), operation.getColumn() ) ) {
			Statement statement = getEntityQueries( entityKey.getMetadata(), tupleContext ).updateEmbeddedColumnStatement( entityKey.getColumnValues(), operation.getColumn(), operation.getValue() );
			statements.add( statement );
		}
		else {
			putProperty( entityKey, node, operation );
		}
	}

	private void putOneToOneAssociation(EntityKey ownerKey, Tuple tuple, Map<String, Object> node, Map<String, EntityKey> toOneAssociations, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		String associationRole = tupleContext.getTupleTypeContext().getRole( operation.getColumn() );

		if ( !processedAssociationRoles.contains( associationRole ) ) {
			processedAssociationRoles.add( associationRole );

			EntityKey targetKey = getEntityKey( tuple, tupleContext.getTupleTypeContext().getAssociatedEntityKeyMetadata( operation.getColumn() ) );

			toOneAssociations.put( associationRole, targetKey );
		}
	}

	private void putProperty(EntityKey entityKey, Map<String, Object> node, TupleOperation operation) {
		node.put( operation.getColumn(), operation.getValue() );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		getEntityQueries( key.getMetadata(), tupleContext ).removeEntity( transaction( tupleContext ), key.getColumnValues() );
	}

	@Override
	public Association getAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		EntityKey entityKey = associationKey.getEntityKey();
		Transaction tx = transaction( associationContext );
		NodeWithEmbeddedNodes node = getEntityQueries( entityKey.getMetadata(), associationContext ).findEntity( tx, entityKey.getColumnValues() );
		if ( node == null ) {
			return null;
		}

		Map<RowKey, Tuple> tuples = createAssociationMap( associationKey, associationContext, entityKey );
		return new Association( new RemoteNeo4jAssociationSnapshot( tuples ) );
	}

	private Map<RowKey, Tuple> createAssociationMap(AssociationKey associationKey, AssociationContext associationContext, EntityKey entityKey) {
		String relationshipType = associationContext.getAssociationTypeContext().getRoleOnMainSide();
		Map<RowKey, Tuple> tuples = new HashMap<RowKey, Tuple>();

		Transaction tx = transaction( associationContext );
		ClosableIterator<RemoteNeo4jAssociationPropertiesRow> relationships = getEntityQueries( entityKey.getMetadata(), associationContext )
				.findAssociation( tx, entityKey.getColumnValues(), relationshipType, associationKey.getMetadata() );
		while ( relationships.hasNext() ) {
			RemoteNeo4jAssociationPropertiesRow row = relationships.next();
			AssociatedEntityKeyMetadata associatedEntityKeyMetadata = associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata();
			RemoteNeo4jTupleAssociationSnapshot snapshot = new RemoteNeo4jTupleAssociationSnapshot( row, associationKey, associatedEntityKeyMetadata );
			RowKey rowKey = convert( associationKey, snapshot );
			tuples.put( rowKey, new Tuple( snapshot, SnapshotType.UPDATE ) );
		}
		return tuples;
	}

	@Override
	protected RowKey convert(AssociationKey associationKey, TupleSnapshot snapshot) {
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

	private void removeAssociationOperation(AssociationKey associationKey, AssociationOperation action, AssociationContext associationContext) {
		Transaction tx = transaction( associationContext );
		getAssociationQueries( associationKey.getMetadata() ).removeAssociationRow( tx, associationKey, action.getKey() );
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
		Transaction tx = transaction( associationContext );
		Tuple associationRow = action.getValue();
		EntityKey embeddedKey = getEntityKey( associationRow, associatedEntityKeyMetadata  );
		if ( !emptyNode( embeddedKey ) ) {
			Object[] relationshipProperties = relationshipProperties( associationKey, action );

			getAssociationQueries( associationKey.getMetadata() )
					.createRelationshipForEmbeddedAssociation( tx, associationKey, embeddedKey, relationshipProperties );
		}
	}

	private static boolean emptyNode(EntityKey entityKey) {
		for ( Object value : entityKey.getColumnValues() ) {
			if ( value != null ) {
				return false;
			}
		}
		return true;
	}

	private Relationship findOrCreateRelationshipWithEntityNode(AssociationKey associationKey, AssociationContext associationContext, AssociationOperation action) {
		Tuple associationRow = action.getValue();
		EntityKey ownerKey = associationKey.getEntityKey();
		AssociatedEntityKeyMetadata associatedEntityKeyMetadata = associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata();
		EntityKey targetKey = getEntityKey( associationRow, associatedEntityKeyMetadata  );
		Object[] relationshipProperties = relationshipProperties( associationKey, associationRow );
		Transaction txId = transaction( associationContext );

		return getAssociationQueries( associationKey.getMetadata() )
			.createRelationship(  txId, ownerKey.getColumnValues(), targetKey.getColumnValues(), relationshipProperties );
	}

	private Object[] relationshipProperties(AssociationKey associationKey, AssociationOperation action) {
		Object[] relationshipProperties = new Object[associationKey.getMetadata().getRowKeyIndexColumnNames().length];
		String[] indexColumns = associationKey.getMetadata().getRowKeyIndexColumnNames();
		for ( int i = 0; i < indexColumns.length; i++ ) {
			relationshipProperties[i] = action.getValue().get( indexColumns[i] );
		}
		return relationshipProperties;
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
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		// If this is the inverse side of a bi-directional association, we don't manage the relationship from this side
		if ( key.getMetadata().isInverse() ) {
			return;
		}

		Transaction tx = transaction( associationContext );
		getAssociationQueries( key.getMetadata() ).removeAssociation( tx, key );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return sequenceGenerator.nextValue( request );
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
		DatastoreProvider datastoreProvider = getServiceRegistry().getService( DatastoreProvider.class );
		BoltNeo4jDatastoreProvider neo4jProvider = (BoltNeo4jDatastoreProvider) datastoreProvider;
		BoltNeo4jClient client = neo4jProvider.getClient();
		BoltTuplesSupplier tupleSupplier = new BoltTuplesSupplier( getEntityQueries( entityKeyMetadata, tupleTypeContext ), entityKeyMetadata, tupleTypeContext, client );
		consumer.consume( tupleSupplier );
	}

	private static class BoltTuplesSupplier implements TuplesSupplier {

		private final BoltNeo4jEntityQueries entityQueries;
		private final EntityKeyMetadata entityKeyMetadata;
		private final TupleTypeContext tupleTypeContext;
		private final BoltNeo4jClient boltClient;

		public BoltTuplesSupplier(
				BoltNeo4jEntityQueries entityQueries,
				EntityKeyMetadata entityKeyMetadata,
				TupleTypeContext tupleTypeContext,
				BoltNeo4jClient boltClient) {
			this.entityQueries = entityQueries;
			this.entityKeyMetadata = entityKeyMetadata;
			this.tupleTypeContext = tupleTypeContext;
			this.boltClient = boltClient;
		}

		@Override
		public ClosableIterator<Tuple> get(TransactionContext transactionContext) {
			boolean shouldCloseTransaction = transactionContext == null;
			Transaction tx = transaction( transactionContext );
			ClosableIterator<NodeWithEmbeddedNodes> entities = entityQueries.findEntitiesWithEmbedded( tx );
			return new BoltNeo4jNodesTupleIterator( tx, entityQueries, entityKeyMetadata, tupleTypeContext, entities, shouldCloseTransaction );
		}

		private Transaction transaction(TransactionContext transactionContext) {
			if ( transactionContext == null ) {
				Session session = boltClient.getDriver().session();
				return session.beginTransaction();
			}
			return (Transaction) transactionContext.getTransactionId();
		}
	}
}
