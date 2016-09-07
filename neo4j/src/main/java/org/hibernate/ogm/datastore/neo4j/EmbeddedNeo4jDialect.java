/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j;

import static org.hibernate.ogm.util.impl.EmbeddedHelper.split;
import static org.neo4j.graphdb.DynamicRelationshipType.withName;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jAssociationQueries;
import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jEntityQueries;
import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jMapsTupleIterator;
import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jNodesTupleIterator;
import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jTupleAssociationSnapshot;
import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jTupleSnapshot;
import org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl.EmbeddedNeo4jTypeConverter;
import org.hibernate.ogm.datastore.neo4j.embedded.impl.EmbeddedNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.logging.impl.GraphLogger;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.OperationContext;
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
import org.hibernate.ogm.model.spi.EntityMetadataInformation;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.QueryExecutionException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.kernel.api.exceptions.schema.UniquePropertyConstraintViolationKernelException;

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
public class EmbeddedNeo4jDialect extends BaseNeo4jDialect {

	private static final Log log = LoggerFactory.getLogger();

	private final GraphDatabaseService dataBase;

	private final EmbeddedNeo4jSequenceGenerator sequenceGenerator;

	private Map<EntityKeyMetadata, EmbeddedNeo4jEntityQueries> entityQueries;

	private Map<AssociationKeyMetadata, EmbeddedNeo4jAssociationQueries> associationQueries;

	public EmbeddedNeo4jDialect(EmbeddedNeo4jDatastoreProvider provider) {
		super( EmbeddedNeo4jTypeConverter.INSTANCE );
		this.dataBase = provider.getDatabase();
		this.sequenceGenerator = provider.getSequenceGenerator();
	}

	@Override
	public void sessionFactoryCreated(SessionFactoryImplementor sessionFactoryImplementor) {
		this.associationQueries = Collections.unmodifiableMap( initializeAssociationQueries( sessionFactoryImplementor ) );
		this.entityQueries = Collections.unmodifiableMap( initializeEntityQueries( sessionFactoryImplementor, associationQueries ) );
	}

	private Map<EntityKeyMetadata, EmbeddedNeo4jEntityQueries> initializeEntityQueries(SessionFactoryImplementor sessionFactoryImplementor,
			Map<AssociationKeyMetadata, EmbeddedNeo4jAssociationQueries> associationQueries) {
		Map<EntityKeyMetadata, EmbeddedNeo4jEntityQueries> entityQueries = initializeEntityQueries( sessionFactoryImplementor );
		for ( AssociationKeyMetadata associationKeyMetadata : associationQueries.keySet() ) {
			EntityKeyMetadata entityKeyMetadata = associationKeyMetadata.getAssociatedEntityKeyMetadata().getEntityKeyMetadata();
			if ( !entityQueries.containsKey( entityKeyMetadata ) ) {
				// Embeddables metadata
				entityQueries.put( entityKeyMetadata, new EmbeddedNeo4jEntityQueries( entityKeyMetadata ) );
			}
		}
		return entityQueries;
	}

	private Map<EntityKeyMetadata, EmbeddedNeo4jEntityQueries> initializeEntityQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<EntityKeyMetadata, EmbeddedNeo4jEntityQueries> queryMap = new HashMap<EntityKeyMetadata, EmbeddedNeo4jEntityQueries>();
		Collection<EntityPersister> entityPersisters = sessionFactoryImplementor.getEntityPersisters().values();
		for ( EntityPersister entityPersister : entityPersisters ) {
			if (entityPersister instanceof OgmEntityPersister ) {
				OgmEntityPersister ogmEntityPersister = (OgmEntityPersister) entityPersister;
				queryMap.put( ogmEntityPersister.getEntityKeyMetadata(), new EmbeddedNeo4jEntityQueries( ogmEntityPersister.getEntityKeyMetadata() ) );
			}
		}
		return queryMap;
	}

	private Map<AssociationKeyMetadata, EmbeddedNeo4jAssociationQueries> initializeAssociationQueries(SessionFactoryImplementor sessionFactoryImplementor) {
		Map<AssociationKeyMetadata, EmbeddedNeo4jAssociationQueries> queryMap = new HashMap<AssociationKeyMetadata, EmbeddedNeo4jAssociationQueries>();
		Collection<CollectionPersister> collectionPersisters = sessionFactoryImplementor.getCollectionPersisters().values();
		for ( CollectionPersister collectionPersister : collectionPersisters ) {
			if ( collectionPersister instanceof OgmCollectionPersister ) {
				OgmCollectionPersister ogmCollectionPersister = (OgmCollectionPersister) collectionPersister;
				EntityKeyMetadata ownerEntityKeyMetadata = ( (OgmEntityPersister) ( ogmCollectionPersister.getOwnerEntityPersister() ) ).getEntityKeyMetadata();
				AssociationKeyMetadata associationKeyMetadata = ogmCollectionPersister.getAssociationKeyMetadata();
				queryMap.put( associationKeyMetadata, new EmbeddedNeo4jAssociationQueries( ownerEntityKeyMetadata, associationKeyMetadata ) );
			}
		}
		return queryMap;
	}

	@Override
	public Tuple getTuple(EntityKey key, OperationContext context) {
		Node entityNode = entityQueries.get( key.getMetadata() ).findEntity( dataBase, key.getColumnValues() );
		if ( entityNode == null ) {
			return null;
		}

		return new Tuple(
				EmbeddedNeo4jTupleSnapshot.fromNode(
						entityNode,
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

		// We only supports one metadata for now
		EntityKeyMetadata metadata = keys[0].getMetadata();
		// The result returned by the query might not be in the same order as the keys.
		ResourceIterator<Node> nodes = entityQueries.get( metadata ).findEntities( dataBase, keys );
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
	private List<Tuple> tuplesResult(EntityKey[] keys, TupleContext tupleContext, ResourceIterator<Node> nodes) {
		// The list is initialized with null because some keys might not have a corresponding node
		Tuple[] tuples = new Tuple[keys.length];
		while ( nodes.hasNext() ) {
			Node node = nodes.next();
			for ( int i = 0; i < keys.length; i++ ) {
				if ( matches( node, keys[i].getColumnNames(), keys[i].getColumnValues() ) ) {
					tuples[i] = new Tuple( EmbeddedNeo4jTupleSnapshot.fromNode( node,
							tupleContext.getTupleTypeContext().getAllAssociatedEntityKeyMetadata(),
							tupleContext.getTupleTypeContext().getAllRoles(),
							keys[i].getMetadata() ) );
					// We assume there are no duplicated keys
					break;
				}
			}
		}
		return Arrays.asList( tuples );
	}

	private boolean matches(Node node, String[] properties, Object[] values) {
		for ( int i = 0; i < properties.length; i++ ) {
			if ( node.hasProperty( properties[i] ) && !node.getProperty( properties[i] ).equals( values[i] )) {
				return false;
			}
			else if ( !node.hasProperty( properties[i] ) && values[i] != null ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext tupleContext) {
		return new Tuple( EmbeddedNeo4jTupleSnapshot.emptySnapshot( key.getMetadata() ) );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
		Tuple tuple = tuplePointer.getTuple();
		EmbeddedNeo4jTupleSnapshot snapshot = (EmbeddedNeo4jTupleSnapshot) tuple.getSnapshot();

		// insert
		if ( snapshot.isNew() ) {
			Node node = insertTuple( key, tuple );
			snapshot.setNode( node );
			applyTupleOperations( key, tuple, node, tuple.getOperations(), tupleContext );
			GraphLogger.log( "Inserted node: %1$s", node );
		}
		// update
		else {
			Node node = snapshot.getNode();
			applyTupleOperations( key, tuple, node, tuple.getOperations(), tupleContext );
			GraphLogger.log( "Updated node: %1$s", node );
		}
	}

	private Node insertTuple(EntityKey key, Tuple tuple) {
		try {
			return entityQueries.get( key.getMetadata() ).insertEntity( dataBase, key.getColumnValues() );
		}
		catch (QueryExecutionException qee) {
			if ( CONSTRAINT_VIOLATION_CODE.equals( qee.getStatusCode() ) ) {
				Throwable cause = findRecognizableCause( qee );
				if ( cause instanceof UniquePropertyConstraintViolationKernelException ) {
					throw new TupleAlreadyExistsException( key.getMetadata(), tuple, qee );
				}
			}
			throw qee;
		}
	}

	private Throwable findRecognizableCause(QueryExecutionException qee) {
		Throwable cause = qee.getCause();
		while ( cause.getCause() != null ) {
			cause = cause.getCause();
		}
		return cause;
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		entityQueries.get( key.getMetadata() ).removeEntity( dataBase, key.getColumnValues() );
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
		EntityKey embeddedKey = getEntityKey( associationRow, associatedEntityKeyMetadata );
		Relationship relationship = associationQueries.get( associationKey.getMetadata() )
				.createRelationshipForEmbeddedAssociation( dataBase, associationKey, embeddedKey );
		applyProperties( associationKey, associationRow, relationship );
		return relationship;
	}

	private Relationship findOrCreateRelationshipWithEntityNode(AssociationKey associationKey, Tuple associationRow, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		EntityKey targetEntityKey = getEntityKey( associationRow, associatedEntityKeyMetadata );
		Node targetNode = entityQueries.get( targetEntityKey.getMetadata() ).findEntity( dataBase, targetEntityKey.getColumnValues() );
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
		Node ownerNode = entityQueries.get( entityKey.getMetadata() ).findEntity( dataBase, entityKey.getColumnValues() );
		Relationship relationship = ownerNode.createRelationshipTo( targetNode, withName( associationKey.getMetadata().getCollectionRole() ) );
		applyProperties( associationKey, associationRow, relationship );
		return relationship;
	}

	@Override
	public Association getAssociation(AssociationKey associationKey, AssociationContext associationContext) {
		EntityKey entityKey = associationKey.getEntityKey();
		Node entityNode = entityQueries.get( entityKey.getMetadata() ).findEntity( dataBase, entityKey.getColumnValues() );
		GraphLogger.log( "Found owner node: %1$s", entityNode );
		if ( entityNode == null ) {
			return null;
		}

		Map<RowKey, Tuple> tuples = createAssociationMap( associationKey, associationContext, entityKey );
		return new Association( new EmbeddedNeo4jAssociationSnapshot( tuples ) );
	}

	private Map<RowKey, Tuple> createAssociationMap(AssociationKey associationKey, AssociationContext associationContext, EntityKey entityKey) {
		String relationshipType = associationContext.getAssociationTypeContext().getRoleOnMainSide();
		ResourceIterator<Relationship> relationships = entityQueries.get( entityKey.getMetadata() )
				.findAssociation( dataBase, entityKey.getColumnValues(), relationshipType );

		Map<RowKey, Tuple> tuples = new HashMap<RowKey, Tuple>();
		try {
			while ( relationships.hasNext() ) {
				Relationship relationship = relationships.next();
				AssociatedEntityKeyMetadata associatedEntityKeyMetadata = associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata();
				EmbeddedNeo4jTupleAssociationSnapshot snapshot = new EmbeddedNeo4jTupleAssociationSnapshot( relationship, associationKey, associatedEntityKeyMetadata );
				RowKey rowKey = convert( associationKey, snapshot );
				tuples.put( rowKey, new Tuple( snapshot ) );
			}
			return tuples;
		}
		finally {
			relationships.close();
		}
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

		associationQueries.get( key.getMetadata() ).removeAssociation( dataBase, key );
	}

	private void applyAssociationOperation(Association association, AssociationKey key, AssociationOperation operation, AssociationContext associationContext) {
		switch ( operation.getType() ) {
		case CLEAR:
			removeAssociation( key, associationContext );
			break;
		case PUT:
			putAssociationOperation( association, key, operation, associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata() );
			break;
		case REMOVE:
			removeAssociationOperation( association, key, operation, associationContext.getAssociationTypeContext().getAssociatedEntityKeyMetadata() );
			break;
		}
	}

	private void putAssociationOperation(Association association, AssociationKey associationKey, AssociationOperation action, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		Relationship relationship = associationQueries.get( associationKey.getMetadata() ).findRelationship( dataBase, associationKey, action.getKey() );

		if (relationship != null) {
			for ( String relationshipProperty : associationKey.getMetadata().getRowKeyIndexColumnNames() ) {
				relationship.setProperty( relationshipProperty, action.getValue().get( relationshipProperty ) );
			}

			for ( String column : associationKey.getMetadata().getColumnsWithoutKeyColumns( action.getValue().getColumnNames() ) ) {
				if ( !isRowKeyColumn( associationKey.getMetadata(), column ) ) {
					relationship.getEndNode().setProperty( column, action.getValue().get( column ) );
				}
			}

			GraphLogger.log( "Updated relationship: %1$s", relationship );
		}
		else {
			relationship = createRelationship( associationKey, action.getValue(), associatedEntityKeyMetadata );
			GraphLogger.log( "Created relationship: %1$s", relationship );
		}
	}

	// TODO replace with method provided by OGM-1035
	private boolean isRowKeyColumn(AssociationKeyMetadata metadata, String column) {
		for ( String rowKeyColumn : metadata.getRowKeyColumnNames() ) {
			if ( rowKeyColumn.equals( column ) ) {
				return true;
			}
		}

		return false;
	}

	private void removeAssociationOperation(Association association, AssociationKey associationKey, AssociationOperation action, AssociatedEntityKeyMetadata associatedEntityKeyMetadata) {
		associationQueries.get( associationKey.getMetadata() ).removeAssociationRow( dataBase, associationKey, action.getKey() );
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
			removeTupleOperation( entityKey, node, operation, tupleContext, processedAssociationRoles );
			break;
		}
	}

	private void removeTupleOperation(EntityKey entityKey, Node node, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		if ( !tupleContext.getTupleTypeContext().isPartOfAssociation( operation.getColumn() ) ) {
			if ( isPartOfRegularEmbedded( entityKey.getColumnNames(), operation.getColumn() ) ) {
				// Embedded node
				String[] split = split( operation.getColumn() );
				removePropertyForEmbedded( node, split, 0 );
			}
			else  if ( node.hasProperty( operation.getColumn() ) ) {
				node.removeProperty( operation.getColumn() );
			}
		}
		// if the column represents a to-one association, remove the relationship
		else {
			String associationRole = tupleContext.getTupleTypeContext().getRole( operation.getColumn() );
			if ( !processedAssociationRoles.contains( associationRole ) ) {

				Iterator<Relationship> relationships = node.getRelationships( withName( associationRole ) ).iterator();

				if ( relationships.hasNext() ) {
					relationships.next().delete();
				}
			}
		}
	}

	/*
	 * It will remove a property from an embedded node if it exists.
	 * After deleting the property, if the node does not have any more properties and relationships (except for an incoming one),
	 * it will delete the embedded node as well.
	 */
	private void removePropertyForEmbedded(Node embeddedNode, String[] embeddedColumnSplit, int i) {
		if ( i == embeddedColumnSplit.length - 1 ) {
			// Property
			String property = embeddedColumnSplit[embeddedColumnSplit.length - 1];
			if ( embeddedNode.hasProperty( property ) ) {
				embeddedNode.removeProperty( property );
			}
		}
		else {
			Iterator<Relationship> iterator = embeddedNode.getRelationships( Direction.OUTGOING, withName( embeddedColumnSplit[i] ) ).iterator();
			if ( iterator.hasNext() ) {
				removePropertyForEmbedded( iterator.next().getEndNode(), embeddedColumnSplit, i + 1 );
			}
		}
		if ( !embeddedNode.getPropertyKeys().iterator().hasNext() ) {
			// Node without properties
			Iterator<Relationship> iterator = embeddedNode.getRelationships().iterator();
			if ( iterator.hasNext() ) {
				Relationship relationship = iterator.next();
				if ( !iterator.hasNext() ) {
					// Node with only one relationship and no properties,
					// we can remove it:
					// It means we have removed all the properties from the embedded node
					// and it is NOT an intermediate node like
					// (entity) --> (embedded1) --> (embedded2)
					relationship.delete();
					embeddedNode.delete();
				}
			}
		}
	}

	private void putTupleOperation(EntityKey entityKey, Tuple tuple, Node node, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		if ( tupleContext.getTupleTypeContext().isPartOfAssociation( operation.getColumn() ) ) {
			// the column represents a to-one association, map it as relationship
			putOneToOneAssociation( tuple, node, operation, tupleContext, processedAssociationRoles );
		}
		else if ( isPartOfRegularEmbedded( entityKey.getMetadata().getColumnNames(), operation.getColumn() ) ) {
			entityQueries.get( entityKey.getMetadata() ).updateEmbeddedColumn( dataBase, entityKey.getColumnValues(), operation.getColumn(), operation.getValue() );
		}
		else {
			putProperty( entityKey, node, operation );
		}
	}

	private void putProperty(EntityKey entityKey, Node node, TupleOperation operation) {
		try {
			node.setProperty( operation.getColumn(), operation.getValue() );
		}
		catch (ConstraintViolationException e) {
			String message = e.getMessage();
			if ( message.contains( "already exists" ) ) {
				throw log.mustNotInsertSameEntityTwice( String.valueOf( operation ), e );
			}
			else {
				throw log.constraintViolation( entityKey, String.valueOf( operation ), e );
			}
		}
	}

	private void putOneToOneAssociation(Tuple tuple, Node node, TupleOperation operation, TupleContext tupleContext, Set<String> processedAssociationRoles) {
		String associationRole = tupleContext.getTupleTypeContext().getRole( operation.getColumn() );

		if ( !processedAssociationRoles.contains( associationRole ) ) {
			processedAssociationRoles.add( associationRole );

			EntityKey targetKey = getEntityKey( tuple, tupleContext.getTupleTypeContext().getAssociatedEntityKeyMetadata( operation.getColumn() ) );

			// delete the previous relationship if there is one; for a to-one association, the relationship won't have any
			// properties, so the type is uniquely identifying it
			Iterator<Relationship> relationships = node.getRelationships( withName( associationRole ) ).iterator();
			if ( relationships.hasNext() ) {
				relationships.next().delete();
			}

			// create a new relationship
			Node targetNode = entityQueries.get( targetKey.getMetadata() ).findEntity( dataBase, targetKey.getColumnValues() );
			node.createRelationshipTo( targetNode, withName( associationRole ) );
		}
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
		ResourceIterator<Node> queryNodes = entityQueries.get( entityKeyMetadata ).findEntities( dataBase );
		try {
			while ( queryNodes.hasNext() ) {
				Node next = queryNodes.next();
				Tuple tuple = new Tuple( EmbeddedNeo4jTupleSnapshot.fromNode( next,
						tupleTypeContext.getAllAssociatedEntityKeyMetadata(), tupleTypeContext.getAllRoles(),
						entityKeyMetadata ) );
				consumer.consume( tuple );
			}
		}
		finally {
			queryNodes.close();
		}
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return sequenceGenerator.nextValue( request );
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<String> backendQuery, QueryParameters queryParameters, TupleContext tupleContext) {
		Map<String, Object> parameters = getParameters( queryParameters );
		String nativeQuery = buildNativeQuery( backendQuery, queryParameters );
		Result result = dataBase.execute( nativeQuery, parameters );

		EntityMetadataInformation entityMetadataInformation = backendQuery.getSingleEntityMetadataInformationOrNull();
		if ( entityMetadataInformation != null ) {
			return new EmbeddedNeo4jNodesTupleIterator( result, entityMetadataInformation.getEntityKeyMetadata(), tupleContext );
		}
		return new EmbeddedNeo4jMapsTupleIterator( result );
	}

	@Override
	public String parseNativeQuery(String nativeQuery) {
		// We return given Cypher queries as they are; Currently there is no API for validating Cypher queries without
		// actually executing them (see https://github.com/neo4j/neo4j/issues/2766)
		return nativeQuery;
	}
}
