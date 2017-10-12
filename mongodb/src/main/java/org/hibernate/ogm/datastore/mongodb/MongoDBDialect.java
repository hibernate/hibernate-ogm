/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb;

import static java.lang.Boolean.FALSE;
import static org.hibernate.ogm.datastore.document.impl.DotPatternMapHelpers.getColumnSharedPrefixOfAssociatedEntityLink;
import static org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoHelpers.hasField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.hibernate.AssertionFailure;
import org.hibernate.ogm.datastore.document.association.impl.DocumentHelpers;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.impl.DotPatternMapHelpers;
import org.hibernate.ogm.datastore.document.impl.EmbeddableStateFinder;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.MapStorageType;
import org.hibernate.ogm.datastore.document.options.spi.AssociationStorageOption;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.mongodb.configuration.impl.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.AssociationStorageStrategy;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBAssociationSnapshot;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoHelpers;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentStorageType;
import org.hibernate.ogm.datastore.mongodb.options.impl.AssociationDocumentStorageOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl.MongoDBQueryDescriptorBuilder;
import org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl.NativeQueryParser;
import org.hibernate.ogm.datastore.mongodb.type.impl.BinaryAsBsonBinaryGridType;
import org.hibernate.ogm.datastore.mongodb.type.impl.ObjectIdGridType;
import org.hibernate.ogm.datastore.mongodb.type.impl.SerializableAsBinaryGridType;
import org.hibernate.ogm.datastore.mongodb.type.impl.StringAsObjectIdGridType;
import org.hibernate.ogm.datastore.mongodb.type.impl.StringAsObjectIdType;
import org.hibernate.ogm.datastore.mongodb.utils.DocumentUtil;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.spi.GroupedChangesToEntityOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.InsertOrUpdateTupleOperation;
import org.hibernate.ogm.dialect.batch.spi.Operation;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.batch.spi.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.batch.spi.RemoveTupleOperation;
import org.hibernate.ogm.dialect.identity.spi.IdentityColumnAwareGridDialect;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.optimisticlock.spi.OptimisticLockingAwareGridDialect;
import org.hibernate.ogm.dialect.query.spi.BackendQuery;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.query.spi.NoOpParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.dialect.query.spi.QueryParameters;
import org.hibernate.ogm.dialect.query.spi.QueryableGridDialect;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.dialect.spi.TuplesSupplier;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.AssociationType;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.type.impl.ByteStringType;
import org.hibernate.ogm.type.impl.CharacterStringType;
import org.hibernate.ogm.type.impl.StringCalendarDateType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.SerializableToBlobType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParsingResult;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MapReduceIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.CollationAlternate;
import com.mongodb.client.model.CollationCaseFirst;
import com.mongodb.client.model.CollationMaxVariable;
import com.mongodb.client.model.CollationStrength;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.MapReduceAction;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * Each Tuple entry is stored as a property in a MongoDB document.
 *
 * Each association is stored in an association document containing three properties:
 * - the association table name (optionally)
 * - the RowKey column names and values
 * - the tuples as an array of elements
 *
 * Associations can be stored as:
 * - one MongoDB collection per association class. The collection name is prefixed.
 * - one MongoDB collection for all associations (the association table name property in then used)
 * - embed the collection info in the owning entity document is planned but not supported at the moment (OGM-177)
 *
 * Collection of embeddable are stored within the owning entity document under the
 * unqualified collection role
 *
 * In MongoDB is possible to batch operations but only for the creation of new documents
 * and only if they don't have invalid characters in the field name.
 * If these conditions are not met, the MongoDB mechanism for batch operations
 * is not going to be used.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 * @author Alan Fitton &lt;alan at eth0.org.uk&gt;
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Thorsten Möller &lt;thorsten.moeller@sbi.ch&gt;
 * @author Guillaume Smet
 */
public class MongoDBDialect extends BaseGridDialect implements QueryableGridDialect<MongoDBQueryDescriptor>, BatchableGridDialect, IdentityColumnAwareGridDialect, MultigetGridDialect, OptimisticLockingAwareGridDialect {

	public static final String ID_FIELDNAME = "_id";
	public static final String PROPERTY_SEPARATOR = ".";
	public static final String ROWS_FIELDNAME = "rows";
	public static final String TABLE_FIELDNAME = "table";
	public static final String ASSOCIATIONS_COLLECTION_PREFIX = "associations_";

	private static final Log log = LoggerFactory.getLogger();

	private static final List<String> ROWS_FIELDNAME_LIST = Collections.singletonList( ROWS_FIELDNAME );

	/**
	 * Pattern used to recognize a constraint violation on the primary key.
	 *
	 * MongoDB returns an exception with {@code .$_id_ } or {@code _id_ } while Fongo returns an exception with {@code ._id }
	 */
	private static final Pattern PRIMARY_KEY_CONSTRAINT_VIOLATION_MESSAGE = Pattern.compile( ".*[. ]\\$?_id_? .*" );

	private final MongoDBDatastoreProvider provider;
	private final MongoDatabase currentDB;

	public MongoDBDialect(MongoDBDatastoreProvider provider) {
		this.provider = provider;
		this.currentDB = this.provider.getDatabase();
	}

	@Override
	public Tuple getTuple(EntityKey key, OperationContext operationContext) {
		Document found = this.getObject( key, operationContext );
		return createTuple( key, operationContext, found );
	}

	@Override
	public List<Tuple> getTuples(EntityKey[] keys, TupleContext tupleContext) {
		if ( keys.length == 0 ) {
			return Collections.emptyList();
		}

		Object[] searchObjects = new Object[keys.length];
		for ( int i = 0; i < keys.length; i++ ) {
			searchObjects[i] = prepareIdObjectValue( keys[i].getColumnNames(), keys[i].getColumnValues() );
		}

		MongoCursor<Document> cursor = this.getObjects( keys[0].getMetadata(), searchObjects, tupleContext );
		try {
			return tuplesResult( keys, searchObjects, tupleContext, cursor );
		}
		finally {
			if ( cursor != null ) {
				cursor.close();
			}
		}
	}

	/*
	 * This method assumes that the entries in the cursor might not be in the same order as the keys and some keys might
	 * not have a matching result in the db.
	 */
	private static List<Tuple> tuplesResult(EntityKey[] keys, Object[] searchObjects, TupleContext tupleContext, MongoCursor<Document> cursor) {
		// The list is initialized with null because some keys might not have a corresponding value in the cursor
		Tuple[] tuples = new Tuple[searchObjects.length];
		while ( cursor.hasNext() ) {
			Document document = cursor.next();
			for ( int i = 0; i < searchObjects.length; i++ ) {
				if ( document.get( ID_FIELDNAME ).equals( searchObjects[i] ) ) {
					tuples[i] = createTuple( keys[i], tupleContext, document );
					// We assume there are no duplicated keys
					break;
				}
			}
		}
		return Arrays.asList( tuples );
	}

	private static Tuple createTuple(EntityKey key, OperationContext operationContext, Document found) {
		if ( found != null ) {
			return new Tuple( new MongoDBTupleSnapshot( found, key.getMetadata() ), SnapshotType.UPDATE );
		}
		else if ( isInTheInsertionQueue( key, operationContext ) ) {
			// The key has not been inserted in the db but it is in the queue
			return new Tuple( new MongoDBTupleSnapshot( prepareIdObject( key ), key.getMetadata() ), SnapshotType.INSERT );
		}
		else {
			return null;
		}
	}

	@Override
	public Tuple createTuple(EntityKeyMetadata entityKeyMetadata, OperationContext operationContext) {
		return new Tuple( new MongoDBTupleSnapshot( new Document(), entityKeyMetadata ), SnapshotType.INSERT );
	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext OperationContext) {
		Document toSave = prepareIdObject( key );
		return new Tuple( new MongoDBTupleSnapshot( toSave, key.getMetadata() ), SnapshotType.INSERT );
	}

	/**
	 * Returns a {@link Document} representing the entity which embeds the specified association.
	 */
	private Document getEmbeddingEntity(AssociationKey key, AssociationContext associationContext) {
		Document embeddingEntityDocument = associationContext.getEntityTuplePointer().getTuple() != null ?
				( (MongoDBTupleSnapshot) associationContext.getEntityTuplePointer().getTuple().getSnapshot() ).getDbObject() : null;

		if ( embeddingEntityDocument != null ) {
			return embeddingEntityDocument;
		}
		else {
			ReadPreference readPreference = getReadPreference( associationContext );

			MongoCollection<Document> collection = readPreference != null ? getCollection( key.getEntityKey() ).withReadPreference( readPreference ) : getCollection( key.getEntityKey() );
			Document searchObject = prepareIdObject( key.getEntityKey() );
			Document projection = getProjection( key, true );

			return collection.find( searchObject ).projection( projection ).first();
		}
	}

	private Document getObject(EntityKey key, OperationContext operationContext) {
		ReadPreference readPreference = getReadPreference( operationContext );

		MongoCollection<Document> collection = readPreference != null ? getCollection( key ).withReadPreference( readPreference ) : getCollection( key ) ;
		Document searchObject = prepareIdObject( key );
		Document projection = getProjection( operationContext );

		FindIterable<Document> fi = collection.find( searchObject );
		return fi != null ? fi.projection( projection ).first() : null;
	}

	private MongoCursor<Document> getObjects(EntityKeyMetadata entityKeyMetadata, Object[] searchObjects, TupleContext
			tupleContext) {
		ReadPreference readPreference = getReadPreference( tupleContext );

		MongoCollection<Document> collection = readPreference != null ? getCollection( entityKeyMetadata ).withReadPreference( readPreference ) : getCollection( entityKeyMetadata );

		Document projection = getProjection( tupleContext );

		Document query = new Document();
		query.put( ID_FIELDNAME, new Document( "$in", Arrays.asList( searchObjects ) ) );
		return collection.find( query ).projection( projection ).iterator();
	}

	private static Document getProjection(OperationContext operationContext) {
		Set<String> columns = new HashSet<>();
		columns.addAll( operationContext.getTupleTypeContext().getPolymorphicEntityColumns() );
		columns.addAll( operationContext.getTupleTypeContext().getSelectableColumns() );
		return getProjection( new ArrayList<>( columns ) );
	}

	/**
	 * Returns a projection object for specifying the fields to retrieve during a specific find operation.
	 */
	private static Document getProjection(List<String> fieldNames) {
		Document projection = new Document();
		for ( String column : fieldNames ) {
			projection.put( column, 1 );
		}

		return projection;
	}

	/**
	 * Create a Document which represents the _id field.
	 * In case of simple id objects the json representation will look like {_id: "theIdValue"}
	 * In case of composite id objects the json representation will look like {_id: {author: "Guillaume", title: "What this method is used for?"}}
	 *
	 * @param key
	 *
	 * @return the Document which represents the id field
	 */
	private static Document prepareIdObject(EntityKey key) {
		return prepareIdObject( key.getColumnNames(), key.getColumnValues() );
	}

	private static Document prepareIdObject(IdSourceKey key) {
		return prepareIdObject( key.getColumnName(), key.getColumnValue() );
	}

	private static Document prepareIdObject(String columnName, String columnValue) {
		return new Document( ID_FIELDNAME, prepareIdObjectValue( columnName, columnValue ) );
	}

	private static Document prepareIdObject(String[] columnNames, Object[] columnValues) {
		return new Document( ID_FIELDNAME, prepareIdObjectValue( columnNames, columnValues ) );
	}

	private static Object prepareIdObjectValue(String columnName, String columnValue) {
		return columnValue;
	}

	private static Object prepareIdObjectValue(String[] columnNames, Object[] columnValues) {
		if ( columnNames.length == 1 ) {
			return columnValues[0];
		}
		else {
			Document idObject = new Document();
			for ( int i = 0; i < columnNames.length; i++ ) {
				String columnName = columnNames[i];
				Object columnValue = columnValues[i];

				if ( columnName.contains( PROPERTY_SEPARATOR ) ) {
					int dotIndex = columnName.indexOf( PROPERTY_SEPARATOR );
					String shortColumnName = columnName.substring( dotIndex + 1 );
					idObject.put( shortColumnName, columnValue );
				}
				else {
					idObject.put( columnNames[i], columnValue );
				}

			}
			return idObject;
		}
	}

	private MongoCollection<Document> getCollection(String table) {
		return currentDB.getCollection( table );
	}

	private MongoCollection<Document> getCollection( EntityKey key ) {
		return getCollection( key.getTable() );
	}

	private MongoCollection<Document> getCollection(EntityKeyMetadata entityKeyMetadata) {
		return getCollection( entityKeyMetadata.getTable() );
	}

	private MongoCollection<Document> getAssociationCollection(AssociationKey key, AssociationStorageStrategy storageStrategy) {
		if ( storageStrategy == AssociationStorageStrategy.GLOBAL_COLLECTION ) {
			return getCollection( MongoDBConfiguration.DEFAULT_ASSOCIATION_STORE );
		}
		else {
			return getCollection( ASSOCIATIONS_COLLECTION_PREFIX + key.getTable() );
		}
	}

	private static Document getSubQuery(String operator, Document query) {
		return query.get( operator ) != null ? (Document) query.get( operator ) : new Document();
	}

	private static void addSubQuery(String operator, Document query, String column, Object value) {
		Document subQuery = getSubQuery( operator, query );
		query.append( operator, subQuery.append( column, value ) );
	}

	private static void addSetToQuery(Document query, String column, Object value) {
		removeSubQuery( "$unset", query, column );
		addSubQuery( "$set", query, column, value );
	}

	private static void addUnsetToQuery(Document query, String column) {
		removeSubQuery( "$set", query, column );
		addSubQuery( "$unset", query, column, Integer.valueOf( 1 ) );
	}

	private static void removeSubQuery(String operator, Document query, String column) {
		Document subQuery = getSubQuery( operator, query );
		subQuery.remove( column );
		if ( subQuery.isEmpty() ) {
			query.remove( operator );
		}
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
		throw new UnsupportedOperationException( "Method not supported in GridDialect anymore" );
	}

	@Override
	//TODO deal with dotted column names once this method is used for ALL / Dirty optimistic locking
	public boolean updateTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, Tuple tuple, TupleContext tupleContext) {
		Document idObject = prepareIdObject( entityKey );

		for ( String versionColumn : oldLockState.getColumnNames() ) {
			idObject.put( versionColumn, oldLockState.get( versionColumn ) );
		}

		Document updater = objectForUpdate( tuple, tupleContext );
		if ( updater.isEmpty() ) {
			return false;
		}

		Document doc = getCollection( entityKey ).findOneAndUpdate( idObject, updater );

		return doc != null;
	}

	@Override
	public void insertTuple(EntityKeyMetadata entityKeyMetadata, Tuple tuple, TupleContext tupleContext) {
		WriteConcern writeConcern = getWriteConcern( tupleContext );
		Document objectWithId = insertDocument( entityKeyMetadata, tuple, writeConcern );
		String idColumnName = entityKeyMetadata.getColumnNames()[0];
		tuple.put( idColumnName, objectWithId.get( ID_FIELDNAME ) );
	}

	/*
	 * InsertOne the tuple and return an object containing the id in the field ID_FIELDNAME
	 */
	private Document insertDocument(EntityKeyMetadata entityKeyMetadata, Tuple tuple, WriteConcern writeConcern) {
		Document dbObject = objectForInsert( tuple, ( (MongoDBTupleSnapshot) tuple.getSnapshot() ).getDbObject() );
		getCollection( entityKeyMetadata ).withWriteConcern( writeConcern ).insertOne( dbObject );
		return dbObject;
	}

	/**
	 * Creates a Document that can be passed to the MongoDB batch insert function
	 */
	private static Document objectForInsert(Tuple tuple, Document dbObject) {
		MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) tuple.getSnapshot();
		for ( TupleOperation operation : tuple.getOperations() ) {
			String column = operation.getColumn();
			if ( notInIdField( snapshot, column ) ) {
				switch ( operation.getType() ) {
					case PUT:
						MongoHelpers.setValue( dbObject, column, operation.getValue() );
						break;
					case PUT_NULL:
					case REMOVE:
						MongoHelpers.resetValue( dbObject, column );
						break;
					}
			}
		}
		return dbObject;
	}

	private static Document objectForUpdate(Tuple tuple, TupleContext tupleContext) {
		return objectForUpdate( tuple, tupleContext, new Document() );
	}

	private static Document objectForUpdate(Tuple tuple, TupleContext tupleContext, Document updateStatement) {
		MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) tuple.getSnapshot();
		EmbeddableStateFinder embeddableStateFinder = new EmbeddableStateFinder( tuple, tupleContext );
		Set<String> nullEmbeddables = new HashSet<>();

		for ( TupleOperation operation : tuple.getOperations() ) {
			String column = operation.getColumn();
			if ( notInIdField( snapshot, column ) ) {
				switch ( operation.getType() ) {
				case PUT:
					addSetToQuery( updateStatement, column, operation.getValue() );
					break;
				case PUT_NULL:
				case REMOVE:
					// try and find if this column is within an embeddable and if that embeddable is null
					// if true, unset the full embeddable
					String nullEmbeddable = embeddableStateFinder.getOuterMostNullEmbeddableIfAny( column );
					if ( nullEmbeddable != null ) {
						// we have a null embeddable
						if ( ! nullEmbeddables.contains( nullEmbeddable ) ) {
							// we have not processed it yet
							addUnsetToQuery( updateStatement, nullEmbeddable );
							nullEmbeddables.add( nullEmbeddable );
						}
					}
					else {
						// simply unset the column
						addUnsetToQuery( updateStatement, column );
					}
					break;
				}
			}
		}

		return updateStatement;
	}

	private static boolean notInIdField(MongoDBTupleSnapshot snapshot, String column) {
		return !column.equals( ID_FIELDNAME ) && !column.endsWith( PROPERTY_SEPARATOR + ID_FIELDNAME ) && !snapshot.isKeyColumn( column );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		Document toDelete = prepareIdObject( key );
		WriteConcern writeConcern = getWriteConcern( tupleContext );
		MongoCollection<Document> collection = getCollection( key ).withWriteConcern( writeConcern );
		collection.deleteMany( toDelete );
	}

	@Override
	public boolean removeTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, TupleContext tupleContext) {
		Document toDelete = prepareIdObject( entityKey );

		for ( String versionColumn : oldLockState.getColumnNames() ) {
			toDelete.put( versionColumn, oldLockState.get( versionColumn ) );
		}

		MongoCollection<Document> collection = getCollection( entityKey );
		Document deleted = collection.findOneAndDelete( toDelete );

		return deleted != null;
	}

	//not for embedded
	private Document findAssociation(AssociationKey key, AssociationContext associationContext, AssociationStorageStrategy storageStrategy) {
		ReadPreference readPreference = getReadPreference( associationContext );
		final Document associationKeyObject = associationKeyToObject( key, storageStrategy );
		MongoCollection<Document> associationCollection = ( readPreference != null  ? getAssociationCollection( key, storageStrategy ).withReadPreference( readPreference ) : getAssociationCollection( key, storageStrategy ) );

		FindIterable<Document> fi = associationCollection.find( associationKeyObject );
		return fi != null ? ( fi.projection( getProjection( key, false ) ).first() ) : null ;
	}

	private static Document getProjection(AssociationKey key, boolean embedded) {
		if ( embedded ) {
			return getProjection( Collections.singletonList( key.getMetadata().getCollectionRole() ) );
		}
		else {
			return getProjection( ROWS_FIELDNAME_LIST );
		}
	}

	private static boolean isInTheInsertionQueue(EntityKey key, AssociationContext associationContext) {
		OperationsQueue queue = associationContext.getOperationsQueue();
		return queue != null && queue.isInTheInsertionQueue( key );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );

		if ( isEmbeddedAssociation( key ) && isInTheInsertionQueue( key.getEntityKey(), associationContext ) ) {
			// The association is embedded and the owner of the association is in the insertion queue
			Document idObject = prepareIdObject( key.getEntityKey() );
			return new Association( new MongoDBAssociationSnapshot( idObject, key, storageStrategy ) );
		}

		// We need to execute the previous operations first or it won't be able to find the key that should have
		// been created
		executeBatch( associationContext.getOperationsQueue() );
		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			Document entity = getEmbeddingEntity( key, associationContext );

			if ( entity != null && hasField( entity, key.getMetadata().getCollectionRole() ) ) {
				return new Association( new MongoDBAssociationSnapshot( entity, key, storageStrategy ) );
			}
			else {
				return null;
			}
		}
		final Document result = findAssociation( key, associationContext, storageStrategy );
		if ( result == null ) {
			return null;
		}
		else {
			return new Association( new MongoDBAssociationSnapshot( result, key, storageStrategy ) );
		}
	}

	private static boolean isEmbeddedAssociation(AssociationKey key) {
		return AssociationKind.EMBEDDED_COLLECTION == key.getMetadata().getAssociationKind();
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );

		Document document = storageStrategy == AssociationStorageStrategy.IN_ENTITY
				? getEmbeddingEntity( key, associationContext )
				: associationKeyToObject( key, storageStrategy );

		Association association = new Association( new MongoDBAssociationSnapshot( document, key, storageStrategy ) );
		// in the case of an association stored in the entity structure, we might end up with rows present in the
		// current snapshot of the entity while we want an empty association here. So, in this case, we clear the
		// snapshot to be sure the association created is empty.
		if ( !association.isEmpty() ) {
			association.clear();
		}

		return association;
	}

	/**
	 * Returns the rows of the given association as to be stored in the database. The return value is one of the
	 * following:
	 * <ul>
	 * <li>A list of plain values such as {@code String}s, {@code int}s etc. in case there is exactly one row key column
	 * which is not part of the association key (in this case we don't need to persist the key name as it can be
	 * restored from the association key upon loading) or</li>
	 * <li>A list of {@code Document}s with keys/values for all row key columns which are not part of the association
	 * key</li>
	 * <li>A {@link Document} with a key for each entry in case the given association has exactly one row key column
	 * which is of type {@code String} (e.g. a hash map) and {@link DocumentStoreProperties#MAP_STORAGE} is not set to
	 * {@link MapStorageType#AS_LIST}. The map values will either be plain values (in case it's single values) or
	 * another {@code Document}.
	 * </ul>
	 */
	private static Object getAssociationRows(Association association, AssociationKey key, AssociationContext associationContext) {
		boolean organizeByRowKey = DotPatternMapHelpers.organizeAssociationMapByRowKey( association, key, associationContext );

		// transform map entries such as ( addressType='home', address_id=123) into the more
		// natural ( { 'home'=123 }
		if ( organizeByRowKey ) {
			String rowKeyColumn = organizeByRowKey ? key.getMetadata().getRowKeyIndexColumnNames()[0] : null;
			Document rows = new Document();

			for ( RowKey rowKey : association.getKeys() ) {
				Document row = (Document) getAssociationRow( association.get( rowKey ), key );

				String rowKeyValue = (String) row.remove( rowKeyColumn );

				// if there is a single column on the value side left, unwrap it
				if ( row.keySet().size() == 1 ) {
					rows.put( rowKeyValue, DocumentUtil.toMap( row ).values().iterator().next() );
				}
				else {
					rows.put( rowKeyValue, row );
				}
			}

			return rows;
		}
		// non-map rows can be taken as is
		else {
			List<Object> rows = new ArrayList<>();

			for ( RowKey rowKey : association.getKeys() ) {
				rows.add( getAssociationRow( association.get( rowKey ), key ) );
			}

			return rows;
		}
	}

	private static Object getAssociationRow(Tuple row, AssociationKey associationKey) {
		String[] rowKeyColumnsToPersist = associationKey.getMetadata().getColumnsWithoutKeyColumns( row.getColumnNames() );

		// return value itself if there is only a single column to store
		if ( rowKeyColumnsToPersist.length == 1 ) {
			return row.get( rowKeyColumnsToPersist[0] );
		}
		// otherwise a Document with the row contents
		else {
			// if the columns are only made of the embedded id columns, remove the embedded id property prefix
			// collectionrole: [ { id: { id1: "foo", id2: "bar" } } ] becomes collectionrole: [ { id1: "foo", id2: "bar" } ]
			String prefix = getColumnSharedPrefixOfAssociatedEntityLink( associationKey );

			Document rowObject = new Document();
			for ( String column : rowKeyColumnsToPersist ) {
				Object value = row.get( column );
				if ( value != null ) {
					// remove the prefix if present
					String columnName = column.startsWith( prefix ) ? column.substring( prefix.length() ) : column;
					MongoHelpers.setValue( rowObject, columnName, value );
				}
			}
			return rowObject;
		}
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
		throw new UnsupportedOperationException( "Method not supported in GridDialect anymore" );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		throw new UnsupportedOperationException( "Method not supported in GridDialect anymore" );
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		String valueColumnName = request.getKey().getMetadata().getValueColumnName();
		MongoCollection<Document> sequenceCollection = getCollection( request.getKey().getTable() );

		Document sequenceId = prepareIdObject( request.getKey() );

		Document setInitialValueOnInsert = new Document();
		addSubQuery( "$setOnInsert", setInitialValueOnInsert, valueColumnName, request.getInitialValue() + request.getIncrement() );

		// I don't trust to make this a constant because the object is not immutable
		FindOneAndUpdateOptions enableUpsert = new FindOneAndUpdateOptions().upsert( true );
		Document originalDocument = sequenceCollection.findOneAndUpdate( sequenceId, setInitialValueOnInsert, enableUpsert );

		Object nextValue = null;
		if ( originalDocument == null ) {
			return request.getInitialValue(); // first time we ask this value
		}
		else {
			// all columns should match to find the value
			Document incrementUpdate = new Document();

			addSubQuery( "$inc", incrementUpdate, valueColumnName, request.getIncrement() );

			Document beforeUpdateDoc = sequenceCollection.findOneAndUpdate( sequenceId, incrementUpdate,  new FindOneAndUpdateOptions().upsert( true ) );

			nextValue = beforeUpdateDoc.get( valueColumnName );
		}
		return (Number) nextValue;
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return getAssociationStorageStrategy( associationKeyMetadata, associationTypeContext ) == AssociationStorageStrategy.IN_ENTITY;
	}

	@Override
	public GridType overrideType(Type type) {
		// Override handling of calendar types
		if ( type == StandardBasicTypes.CALENDAR || type == StandardBasicTypes.CALENDAR_DATE ) {
			return StringCalendarDateType.INSTANCE;
		}
		else if ( type == StandardBasicTypes.BINARY ) {
			return BinaryAsBsonBinaryGridType.INSTANCE;
		}
		else if ( type == StandardBasicTypes.BYTE ) {
			return ByteStringType.INSTANCE;
		}
		else if ( type == StandardBasicTypes.CHARACTER ) {
			return CharacterStringType.INSTANCE;
		}
		else if ( type.getReturnedClass() == ObjectId.class ) {
			return ObjectIdGridType.INSTANCE;
		}
		else if ( type instanceof StringAsObjectIdType ) {
			return StringAsObjectIdGridType.INSTANCE;
		}
		else if ( type instanceof SerializableToBlobType ) {
			SerializableToBlobType<?> exposedType = (SerializableToBlobType<?>) type;
			return new SerializableAsBinaryGridType<>( exposedType.getJavaTypeDescriptor() );
		}
		else if ( type instanceof MaterializedBlobType ) {
			MaterializedBlobType exposedType = (MaterializedBlobType) type;
			return new SerializableAsBinaryGridType<>( exposedType.getJavaTypeDescriptor() );
		}
		return null; // all other types handled as in hibernate-ogm-core
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
		MongoDatabase db = provider.getDatabase();
		MongoCollection<Document> collection = db.getCollection( entityKeyMetadata.getTable() );
		consumer.consume( new MongoDBTuplesSupplier( collection, entityKeyMetadata ) );
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<MongoDBQueryDescriptor> backendQuery, QueryParameters queryParameters, TupleContext tupleContext) {
		MongoDBQueryDescriptor queryDescriptor = backendQuery.getQuery();

		EntityKeyMetadata entityKeyMetadata =
				backendQuery.getSingleEntityMetadataInformationOrNull() == null ? null :
					backendQuery.getSingleEntityMetadataInformationOrNull().getEntityKeyMetadata();

		String collectionName = getCollectionName( backendQuery, queryDescriptor, entityKeyMetadata );
		MongoCollection<Document> collection = provider.getDatabase().getCollection( collectionName );

		if ( !queryParameters.getPositionalParameters().isEmpty() ) { // TODO Implement binding positional parameters.
			throw new UnsupportedOperationException( "Positional parameters are not yet supported for MongoDB native queries." );
		}

		switch ( queryDescriptor.getOperation() ) {
			case FIND:
				return doFind( queryDescriptor, queryParameters, collection, entityKeyMetadata );
			case FINDONE:
				return doFindOne( queryDescriptor, collection, entityKeyMetadata );
			case FINDANDMODIFY:
				return doFindAndModify( queryDescriptor, collection, entityKeyMetadata );
			case AGGREGATE:
				return doAggregate( queryDescriptor, queryParameters, collection, entityKeyMetadata );
			case AGGREGATE_PIPELINE:
				return doAggregatePipeline( queryDescriptor, queryParameters, collection, entityKeyMetadata );
			case COUNT:
				return doCount( queryDescriptor, collection );
			case DISTINCT:
				return doDistinct( queryDescriptor, collection );
			case MAP_REDUCE:
				return doMapReduce( queryDescriptor, collection );
			case INSERT:
			case REMOVE:
			case UPDATE:
			case UPDATEONE:
				throw log.updateQueryMustBeExecutedViaExecuteUpdate( queryDescriptor );
			default:
				throw new IllegalArgumentException( "Unexpected query operation: " + queryDescriptor );
		}
	}

	@Override
	public int executeBackendUpdateQuery(final BackendQuery<MongoDBQueryDescriptor> backendQuery, final QueryParameters queryParameters, final TupleContext tupleContext) {
		MongoDBQueryDescriptor queryDescriptor = backendQuery.getQuery();

		EntityKeyMetadata entityKeyMetadata =
				backendQuery.getSingleEntityMetadataInformationOrNull() == null ? null :
					backendQuery.getSingleEntityMetadataInformationOrNull().getEntityKeyMetadata();

		String collectionName = getCollectionName( backendQuery, queryDescriptor, entityKeyMetadata );
		MongoCollection<Document> collection = provider.getDatabase().getCollection( collectionName );

		if ( !queryParameters.getPositionalParameters().isEmpty() ) { // TODO Implement binding positional parameters.
			throw new UnsupportedOperationException( "Positional parameters are not yet supported for MongoDB native queries." );
		}

		switch ( queryDescriptor.getOperation() ) {
			case INSERTMANY:
			case INSERTONE:
			case INSERT:
				return doInsert( queryDescriptor, collection );
			case REMOVE:
				return doRemove( queryDescriptor, collection );
			case UPDATE:
				return doUpdate( queryDescriptor, collection );
			case UPDATEONE:
				return doUpdateOne( queryDescriptor, collection );
			case FIND:
			case FINDONE:
			case FINDANDMODIFY:
			case AGGREGATE:
			case AGGREGATE_PIPELINE:
			case COUNT:
			case DISTINCT:
				throw log.readQueryMustBeExecutedViaGetResultList( queryDescriptor );
			default:
				throw new IllegalArgumentException( "Unexpected query operation: " + queryDescriptor );
		}
	}

	@Override
	public MongoDBQueryDescriptor parseNativeQuery(String nativeQuery) {
		NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
		ParsingResult<MongoDBQueryDescriptorBuilder> parseResult = new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() )
				.run( nativeQuery );
		if ( parseResult.hasErrors() ) {
			throw new IllegalArgumentException( "Unsupported native query: " + ErrorUtils.printParseErrors( parseResult.parseErrors ) );
		}

		return parseResult.resultValue.build();
	}

	@Override
	public DuplicateInsertPreventionStrategy getDuplicateInsertPreventionStrategy(EntityKeyMetadata entityKeyMetadata) {
		return DuplicateInsertPreventionStrategy.NATIVE;
	}

	private static ClosableIterator<Tuple> doAggregate(MongoDBQueryDescriptor query, QueryParameters queryParameters, MongoCollection<Document> collection, EntityKeyMetadata entityKeyMetadata) {
		List<Document> pipeline = new ArrayList<Document>();

		pipeline.add( stage( "$match", query.getCriteria() ) );
		pipeline.add( stage( "$project", query.getProjection() ) );

		if ( query.getUnwinds() != null && !query.getUnwinds().isEmpty() ) {
			for ( String field : query.getUnwinds() ) {
				pipeline.add( stage( "$unwind", "$" + field ) );
			}
		}

		if ( query.getOrderBy() != null ) {
			pipeline.add( stage( "$sort", query.getOrderBy() ) );
		}

		applyFirstResult( queryParameters, pipeline );

		applyMaxResults( queryParameters, pipeline );

		AggregateIterable<Document> output = collection.aggregate( pipeline );
		return new MongoDBAggregationOutput( output, entityKeyMetadata );
	}

	private static void applyMaxResults(QueryParameters queryParameters, List<Document> pipeline) {
		if ( queryParameters.getRowSelection().getMaxRows() != null ) {
			pipeline.add( stage( "$limit", queryParameters.getRowSelection().getMaxRows() ) );
		}
	}

	private static void applyFirstResult(QueryParameters queryParameters, List<Document> pipeline) {
		if ( queryParameters.getRowSelection().getFirstRow() != null ) {
			pipeline.add( stage( "$skip", queryParameters.getRowSelection().getFirstRow() ) );
		}
	}

	private static ClosableIterator<Tuple> doAggregatePipeline(MongoDBQueryDescriptor query, QueryParameters queryParameters, MongoCollection<Document> collection, EntityKeyMetadata entityKeyMetadata) {
		List<Document> pipeline = query.getPipeline();
		applyFirstResult( queryParameters, pipeline );
		applyMaxResults( queryParameters, pipeline );
		AggregateIterable<Document> output = collection.aggregate( pipeline );
		return new MongoDBAggregationOutput( output, entityKeyMetadata );
	}

	private static Document stage(String key, Object value) {
		Document stage = new Document();
		stage.put( key, value );
		return stage;
	}

	/**
	 * do 'Distinct' operation
	 *
	 * @param queryDescriptor descriptor of MongoDB query
	 * @param collection collection for execute the operation
	 * @return result iterator
	 * @see <a href ="https://docs.mongodb.com/manual/reference/method/db.collection.distinct/">distinct</a>
	 */
	private static ClosableIterator<Tuple> doDistinct(final MongoDBQueryDescriptor queryDescriptor, final MongoCollection<Document> collection) {
		DistinctIterable<?> distinctFieldValues = collection.distinct( queryDescriptor.getDistinctFieldName(), queryDescriptor.getCriteria(), String.class );
		Collation collation = getCollation( queryDescriptor.getOptions() );

		distinctFieldValues = collation != null ? distinctFieldValues.collation( collation ) : distinctFieldValues;

		MongoCursor<?> cursor = distinctFieldValues.iterator();
		List<Object> documents = new ArrayList<>();
		while ( cursor.hasNext() ) {
			documents.add( cursor.next() );
		}
		MapTupleSnapshot snapshot = new MapTupleSnapshot( Collections.<String, Object>singletonMap( "n", documents ) );
		return CollectionHelper.newClosableIterator( Collections.singletonList( new Tuple( snapshot, SnapshotType.UNKNOWN ) ) );
	}

	/**
	 * do 'Map Reduce' operation
	 * @param queryDescriptor descriptor of MongoDB map reduce query
	 * @param collection collection on which operation will be performed
	 * @return result iterator
	 * @see <a href ="https://docs.mongodb.com/manual/reference/method/db.collection.mapReduce/">MapReduce</a>
	 */
	private static ClosableIterator<Tuple> doMapReduce(final MongoDBQueryDescriptor queryDescriptor, final MongoCollection<Document> collection) {

		MapReduceIterable<Document> mapReduceIterable = collection.mapReduce( queryDescriptor.getMapFunction(), queryDescriptor.getReduceFunction() );
		Document options = queryDescriptor.getOptions();
		if ( options != null ) {
			Document query = (Document) options.get( "query" );
			Document sort = (Document) options.get( "sort" );
			Integer limit = options.getInteger( "limit" );
			String finalizeFunction = options.getString( "finalize" );
			Document scope = (Document) options.get( "scope" );
			Boolean jsMode = options.getBoolean( "jsMode" );
			Boolean verbose = options.getBoolean( "verbose" );
			Boolean bypassDocumentValidation = options.getBoolean( "bypassDocumentValidation" );
			Collation collation = getCollation( (Document) options.get( "collation" ) );
			MapReduceAction mapReduceAction = null;
			String collectionName = null;
			String dbName = null;
			Boolean sharded = null;
			Boolean nonAtomic = null;

			Object out;
			if ( ( out = options.get( "out" ) ) != null ) {
				if ( out instanceof String ) {
					collectionName = (String) out;
				}
				else if ( out instanceof Document ) {
					Document outDocument = (Document) out;
					if ( outDocument.containsKey( "merge" ) ) {
						mapReduceAction = MapReduceAction.MERGE;
						collectionName = outDocument.getString( "merge" );
					}
					else if ( outDocument.containsKey( "replace" ) ) {
						mapReduceAction = MapReduceAction.REPLACE;
						collectionName = outDocument.getString( "replace" );
					}
					else if ( ( (Document) out ).containsKey( "reduce" ) ) {
						mapReduceAction = MapReduceAction.REDUCE;
						collectionName = outDocument.getString( "reduce" );
					}
					dbName = outDocument.getString( "db" );
					sharded = outDocument.getBoolean( "sharded" );
					nonAtomic = outDocument.getBoolean( "nonAtomic" );
				}
			}
			mapReduceIterable = ( query != null ) ? mapReduceIterable.filter( query ) : mapReduceIterable;
			mapReduceIterable = ( sort != null ) ? mapReduceIterable.sort( sort ) : mapReduceIterable;
			mapReduceIterable = ( limit != null ) ? mapReduceIterable.limit( limit ) : mapReduceIterable;
			mapReduceIterable = ( finalizeFunction != null ) ? mapReduceIterable.finalizeFunction( finalizeFunction ) : mapReduceIterable;
			mapReduceIterable = ( scope != null ) ? mapReduceIterable.scope( scope ) : mapReduceIterable;
			mapReduceIterable = ( jsMode != null ) ? mapReduceIterable.jsMode( jsMode ) : mapReduceIterable;
			mapReduceIterable = ( verbose != null ) ? mapReduceIterable.verbose( verbose ) : mapReduceIterable;
			mapReduceIterable = ( bypassDocumentValidation != null ) ? mapReduceIterable.bypassDocumentValidation( bypassDocumentValidation ) : mapReduceIterable;
			mapReduceIterable = ( collation != null ) ? mapReduceIterable.collation( collation ) : mapReduceIterable;
			mapReduceIterable = ( mapReduceAction != null ) ? mapReduceIterable.action( mapReduceAction ) : mapReduceIterable;
			mapReduceIterable = ( collectionName != null ) ? mapReduceIterable.collectionName( collectionName ) : mapReduceIterable;
			mapReduceIterable = ( dbName != null ) ? mapReduceIterable.databaseName( dbName ) : mapReduceIterable;
			mapReduceIterable = ( sharded != null ) ? mapReduceIterable.sharded( sharded ) : mapReduceIterable;
			mapReduceIterable = ( nonAtomic != null ) ? mapReduceIterable.nonAtomic( nonAtomic ) : mapReduceIterable;
		}

		MongoCursor<Document> cursor = mapReduceIterable.iterator();
		Map<Object, Object> documents = new LinkedHashMap<>();
		while ( cursor.hasNext() ) {
			Document doc = cursor.next();
			documents.put( doc.get( "_id" ), doc.get( "value" ) );
		}
		MapTupleSnapshot snapshot = new MapTupleSnapshot( Collections.<String, Object>singletonMap( "n", documents ) );
		return CollectionHelper.newClosableIterator( Collections.singletonList( new Tuple( snapshot, SnapshotType.UNKNOWN ) ) );
	}

	private static Collation getCollation(Document dbObject) {
		Collation collation = null;
		if ( dbObject != null ) {
			String caseFirst = dbObject.getString( "caseFirst" );
			Integer strength = dbObject.getInteger( "strength" );
			String alternate = dbObject.getString( "alternate" );
			String maxVariable = dbObject.getString( "maxVariable" );
			collation =  Collation.builder()
					.locale( (String) dbObject.get( "locale" ) )
					.caseLevel( (Boolean) dbObject.get( "caseLevel" ) )
					.numericOrdering( (Boolean) dbObject.get( "numericOrdering" ) )
					.backwards( (Boolean) dbObject.get( "backwards" ) )
					.collationCaseFirst( caseFirst == null ? null : CollationCaseFirst.fromString( caseFirst ) )
					.collationStrength( strength == null ? null : CollationStrength.fromInt( strength ) )
					.collationAlternate( alternate == null ? null : CollationAlternate.fromString( alternate ) )
					.collationMaxVariable(  maxVariable == null ? null : CollationMaxVariable.fromString( maxVariable ) )
					.build();
		}
		return collation;
	}

	private static ClosableIterator<Tuple> doFind(MongoDBQueryDescriptor query, QueryParameters queryParameters, MongoCollection<Document> collection,
			EntityKeyMetadata entityKeyMetadata) {
		Document criteria = query.getCriteria();
		Document orderby = query.getOrderBy();
		int maxTimeMS = -1;

		Document modifiers = new Document();
		// We need to extract the different parts of the criteria and pass them to the cursor API
		if ( criteria.containsKey( "$query" ) ) {
			if ( orderby == null ) {
				orderby = (Document) criteria.get( "$orderby" );
			}
			maxTimeMS = criteria.getInteger( "$maxTimeMS", -1 );

			addModifier( modifiers, criteria, "$hint" );
			addModifier( modifiers, criteria, "$maxScan" );
			addModifier( modifiers, criteria, "$snapshot", false );
			addModifier( modifiers, criteria, "$min" );
			addModifier( modifiers, criteria, "$max" );
			addModifier( modifiers, criteria, "$comment" );
			addModifier( modifiers, criteria, "$explain", false );

			criteria = (Document) criteria.get( "$query" );
		}

		FindIterable<Document> prepareFind = collection.find( criteria ).modifiers( modifiers ).projection( query.getProjection() );
		if ( orderby != null ) {
			prepareFind.sort( orderby );
		}
		if ( maxTimeMS > 0 ) {
			prepareFind.maxTime( maxTimeMS, TimeUnit.MILLISECONDS );
		}

		// apply firstRow/maxRows if present
		if ( queryParameters.getRowSelection().getFirstRow() != null ) {
			prepareFind.skip( queryParameters.getRowSelection().getFirstRow() );
		}

		if ( queryParameters.getRowSelection().getMaxRows() != null ) {
			prepareFind.limit( queryParameters.getRowSelection().getMaxRows() );
		}

		MongoCursor<Document> iterator = prepareFind.iterator();
		return new MongoDBResultsCursor( iterator, entityKeyMetadata );
	}

	private static void addModifier(Document modifiers, Document criteria, String key) {
		Object value = criteria.get( key );
		if ( value != null ) {
			modifiers.append( key, value );
		}
	}

	private static <T> void addModifier(Document modifiers, Document criteria, String key, T defaultValue) {
		Object value = criteria.get( key );
		if ( value == null ) {
			value = defaultValue;
		}
		modifiers.append( key, value );
	}

	private static ClosableIterator<Tuple> doFindOne(final MongoDBQueryDescriptor query, final MongoCollection<Document> collection,
			final EntityKeyMetadata entityKeyMetadata) {

		final Document theOne = collection.find( query.getCriteria() ).projection( query.getProjection() ).first();
		return new SingleTupleIterator( theOne, collection, entityKeyMetadata );
	}

	private static ClosableIterator<Tuple> doFindAndModify(final MongoDBQueryDescriptor queryDesc, final MongoCollection<Document> collection,
			final EntityKeyMetadata entityKeyMetadata) {

		Document query = (Document) queryDesc.getCriteria().get( "query" );
		Document fields = (Document) queryDesc.getCriteria().get( "fields" );
		Document sort = (Document) queryDesc.getCriteria().get( "sort" );
		Boolean remove = (Boolean) queryDesc.getCriteria().get( "remove" );
		Document update = (Document) queryDesc.getCriteria().get( "update" );
		Boolean returnNewDocument = (Boolean) queryDesc.getCriteria().get( "new" );
		Boolean upsert = (Boolean) queryDesc.getCriteria().get( "upsert" );
		Boolean bypass = (Boolean) queryDesc.getCriteria().get( "bypassDocumentValidation" );
		Document o = (Document) queryDesc.getCriteria().get( "writeConcern" );
		WriteConcern wc = getWriteConcern( o );
		Document theOne = null;
		if ( remove != null ? remove : false ) {
			theOne = collection.findOneAndDelete(
					query,
					new FindOneAndDeleteOptions().sort( sort )
							.projection( fields )
							.maxTime( 0, TimeUnit.MILLISECONDS )
			);
		}
		else {
			FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
					.sort( sort )
					.bypassDocumentValidation( ( bypass != null ? bypass : false ) )
					.upsert( ( upsert != null ? upsert : false ) )
					.projection( fields )
					.returnDocument( ( returnNewDocument != null ? returnNewDocument : false ) ? ReturnDocument.AFTER :	ReturnDocument.BEFORE )
					.maxTime( 0, TimeUnit.MILLISECONDS );
			theOne = collection.withWriteConcern( (wc != null ? wc : collection.getWriteConcern() ) ).findOneAndUpdate( query, update, options );
		}
		return new SingleTupleIterator( theOne, collection, entityKeyMetadata );
	}

	private static int doInsert(final MongoDBQueryDescriptor queryDesc, final MongoCollection<Document> collection) {
		Document options = queryDesc.getOptions();
		Boolean ordered = FALSE;
		WriteConcern wc = null;
		if ( options != null ) {
			ordered = (Boolean) options.get( "ordered" );
			ordered = ( ordered != null ) ? ordered : FALSE;
			Document o = (Document) options.get( "writeConcern" );
			wc = getWriteConcern( o );
		}

		// Need to use BulkWriteOperation here rather than collection.insert(..) because the WriteResult returned
		// by the latter returns 0 for getN() even if the insert was successful (which is bizarre, but that's the way it
		// is defined...)
		List<InsertOneModel<Document>> operationList = null;
		if ( queryDesc.getUpdateOrInsertMany() != null ) {
			operationList = new ArrayList<>( queryDesc.getUpdateOrInsertMany().size() );
			for ( Document doc : queryDesc.getUpdateOrInsertMany() ) {
				operationList.add( new InsertOneModel<>( doc ) );
			}
		}
		else {
			operationList = new ArrayList<>( 1 );
			operationList.add( new InsertOneModel<>( queryDesc.getUpdateOrInsertOne() ) );
		}
		final BulkWriteResult result = collection.withWriteConcern( ( wc != null ? wc : collection.getWriteConcern() ) ).bulkWrite( operationList, new BulkWriteOptions().ordered( ordered ) );

		if ( result.wasAcknowledged() ) {
			return result.getInsertedCount();
		}
		return -1; // Not sure if we should throw an exception instead?
	}

	private static int doRemove(final MongoDBQueryDescriptor queryDesc, final MongoCollection<Document> collection) {
		Document query = queryDesc.getCriteria();
		Document options = queryDesc.getOptions();
		Boolean justOne = FALSE;
		WriteConcern wc = null;
		if ( options != null ) {
			justOne = (Boolean) options.get( "justOne" );
			justOne = ( justOne != null ) ? justOne : FALSE;
			if ( justOne ) { // IMPROVE See https://jira.mongodb.org/browse/JAVA-759
				throw new UnsupportedOperationException( "Using 'justOne' in a remove query is not yet supported." );
			}
			Document o = (Document) options.get( "writeConcern" );
			wc = getWriteConcern( o );
		}

		DeleteResult result = collection.withWriteConcern( ( wc != null ? wc : collection.getWriteConcern() ) ).deleteMany( query );
		if ( result.wasAcknowledged() ) {
			return (int) result.getDeletedCount();
		}
		return -1; // Not sure if we should throw an exception instead?
	}

	private static int doUpdate(final MongoDBQueryDescriptor queryDesc, final MongoCollection<Document> collection) {
		Document query = queryDesc.getCriteria();
		Document update = queryDesc.getUpdateOrInsertOne();
		Document options = queryDesc.getOptions();
		Boolean upsert = FALSE;
		Boolean multi = FALSE;
		WriteConcern wc = null;
		if ( options != null ) {
			upsert = (Boolean) options.get( "upsert" );
			upsert = ( upsert != null ) ? upsert : FALSE;
			multi = (Boolean) options.get( "multi" );
			multi = ( multi != null ) ? multi : FALSE;
			Document o = (Document) options.get( "writeConcern" );
			wc = getWriteConcern( o );
		}
		UpdateOptions updateOptions = new UpdateOptions().upsert( upsert );
		UpdateResult result = null;
		if ( multi ) {
			result = collection.withWriteConcern( ( wc != null ? wc : collection.getWriteConcern() ) ).updateMany( query, update, updateOptions );
		}
		else {
			result = collection.withWriteConcern( ( wc != null ? wc : collection.getWriteConcern() ) ).updateOne( query, update, updateOptions );
		}

		if ( result.wasAcknowledged() ) {
			// IMPROVE How could we return result.getUpsertedId() if it was an upsert, or isUpdateOfExisting()?
			// I see only a possibility by using javax.persistence.StoredProcedureQuery in the application
			// and then using getOutputParameterValue(String) to get additional result values.
			return (int) result.getModifiedCount();
		}
		return -1; // Not sure if we should throw an exception instead?
	}

	private int doUpdateOne(final MongoDBQueryDescriptor queryDescriptor, final MongoCollection<Document> collection) {
		Document query = queryDescriptor.getCriteria();
		Document update = queryDescriptor.getUpdateOrInsertOne();
		Document options = queryDescriptor.getOptions();
		Collation collation = null;
		Boolean upsert = FALSE;
		WriteConcern writeConcern = null;
		if ( options != null ) {
			Document wc = (Document) options.get( "writeConcern" );
			writeConcern = getWriteConcern( wc );

			upsert = (Boolean) options.get( "upsert" );
			upsert = ( upsert != null ) ? upsert : FALSE;

			Document col = (Document) options.get( "collation" );
			collation = ( col != null ) ? getCollation( col ) : null;
		}
		UpdateOptions updateOptions = new UpdateOptions()
				.upsert( upsert )
				.collation( collation );

		UpdateResult updateResult = collection
				.withWriteConcern( writeConcern != null ? writeConcern : collection.getWriteConcern() )
				.updateOne( query, update, updateOptions );

		if ( updateResult.wasAcknowledged() ) {
			return (int) updateResult.getModifiedCount();
		}
		return -1;
	}

	private static ClosableIterator<Tuple> doCount(MongoDBQueryDescriptor query, MongoCollection<Document> collection) {
		long count = collection.count( query.getCriteria() );
		MapTupleSnapshot snapshot = new MapTupleSnapshot( Collections.<String, Object>singletonMap( "n", count ) );
		return CollectionHelper.newClosableIterator( Collections.singletonList( new Tuple( snapshot, SnapshotType.UNKNOWN ) ) );
	}

	/**
	 * @param obj A JSON object representing a write concern.
	 * @return The parsed write concern or <code>null</code> if <code>obj</code> is <code>null</code>.
	 */
	@SuppressWarnings("deprecation")
	private static WriteConcern getWriteConcern(Document obj) {
		WriteConcern wc = null;
		if ( obj != null ) {
			Object w = obj.get( "w" );
			Boolean j = (Boolean) obj.get( "j" );
			Integer t = (Integer) obj.get( "wtimeout" );
			if ( w instanceof String ) {
				wc = new WriteConcern( (String) w, ( t != null ? t : 0 ), false, ( j != null ? j : false ) );
			}
			if ( w instanceof Number ) {
				wc = new WriteConcern( ( (Number) w ).intValue(), ( t != null ? t : 0 ), false, ( j != null ? j : false ) );
			}
		}
		return wc;
	}

	/**
	 * Returns the name of the MongoDB collection to execute the given query against. Will either be retrieved
	 * <ul>
	 * <li>from the given query descriptor (in case the query has been translated from JP-QL or it is a native query
	 * using the extended syntax {@code db.<COLLECTION>.<OPERATION>(...)}</li>
	 * <li>or from the single mapped entity type if it is a native query using the criteria-only syntax
	 *
	 * @param customQuery the original query to execute
	 * @param queryDescriptor descriptor for the query
	 * @param entityKeyMetadata meta-data in case this is a query with exactly one entity return
	 * @return the name of the MongoDB collection to execute the given query against
	 */
	private static String getCollectionName(BackendQuery<?> customQuery, MongoDBQueryDescriptor queryDescriptor, EntityKeyMetadata entityKeyMetadata) {
		if ( queryDescriptor.getCollectionName() != null ) {
			return queryDescriptor.getCollectionName();
		}
		else if ( entityKeyMetadata != null ) {
			return entityKeyMetadata.getTable();
		}
		else {
			throw log.unableToDetermineCollectionName( customQuery.getQuery().toString() );
		}
	}

	private static Document associationKeyToObject(AssociationKey key, AssociationStorageStrategy storageStrategy) {
		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			throw new AssertionFailure( MongoHelpers.class.getName()
					+ ".associationKeyToObject should not be called for associations embedded in entity documents" );
		}
		Object[] columnValues = key.getColumnValues();
		Document columns = new Document();

		// if the columns are only made of the embedded id columns, remove the embedded id property prefix
		// _id: [ { id: { id1: "foo", id2: "bar" } } ] becomes _id: [ { id1: "foo", id2: "bar" } ]
		String prefix = DocumentHelpers.getColumnSharedPrefix( key.getColumnNames() );
		prefix = prefix == null ? "" : prefix + ".";
		int i = 0;
		for ( String name : key.getColumnNames() ) {
			MongoHelpers.setValue( columns, name.substring( prefix.length() ), columnValues[i++] );
		}

		Document idObject = new Document();

		if ( storageStrategy == AssociationStorageStrategy.GLOBAL_COLLECTION ) {
			columns.put( MongoDBDialect.TABLE_FIELDNAME, key.getTable() );
		}
		idObject.put( MongoDBDialect.ID_FIELDNAME, columns );
		return idObject;
	}

	private static AssociationStorageStrategy getAssociationStorageStrategy(AssociationKey key, AssociationContext associationContext) {
		return getAssociationStorageStrategy( key.getMetadata(), associationContext.getAssociationTypeContext() );
	}

	/**
	 * Returns the {@link AssociationStorageStrategy} effectively applying for the given association. If a setting is
	 * given via the option mechanism, that one will be taken, otherwise the default value as given via the
	 * corresponding configuration property is applied.
	 */
	private static AssociationStorageStrategy getAssociationStorageStrategy(AssociationKeyMetadata keyMetadata, AssociationTypeContext associationTypeContext) {
		AssociationStorageType associationStorage = associationTypeContext
				.getOptionsContext()
				.getUnique( AssociationStorageOption.class );

		AssociationDocumentStorageType associationDocumentStorageType = associationTypeContext
				.getOptionsContext()
				.getUnique( AssociationDocumentStorageOption.class );

		return AssociationStorageStrategy.getInstance( keyMetadata, associationStorage, associationDocumentStorageType );
	}

	@Override
	public void executeBatch(OperationsQueue queue) {
		if ( !queue.isClosed() ) {
			Operation operation = queue.poll();
			Map<MongoCollection<Document>, BatchInsertionTask> inserts = new HashMap<MongoCollection<Document>, BatchInsertionTask>();

			List<Tuple> insertTuples = new ArrayList<Tuple>();

			while ( operation != null ) {
				if ( operation instanceof GroupedChangesToEntityOperation ) {
					GroupedChangesToEntityOperation entityOperation = (GroupedChangesToEntityOperation) operation;
					executeBatchUpdate( inserts, insertTuples, entityOperation );
				}
				else if ( operation instanceof RemoveTupleOperation ) {
					RemoveTupleOperation removeTupleOperation = (RemoveTupleOperation) operation;
					executeBatchRemove( inserts, removeTupleOperation );
				}
				else {
					throw new UnsupportedOperationException( "Operation not supported: " + operation.getClass().getSimpleName() );
				}
				operation = queue.poll();
			}

			flushInserts( inserts );
			for ( Tuple insertTuple : insertTuples ) {
				insertTuple.setSnapshotType( SnapshotType.UPDATE );
			}

			queue.clear();
		}
	}

	private void executeBatchRemove(Map<MongoCollection<Document>, BatchInsertionTask> inserts, RemoveTupleOperation tupleOperation) {
		EntityKey entityKey = tupleOperation.getEntityKey();
		MongoCollection<Document> collection = getCollection( entityKey );
		BatchInsertionTask batchedInserts = inserts.get( collection );

		if ( batchedInserts != null && batchedInserts.containsKey( entityKey ) ) {
			batchedInserts.remove( entityKey );
		}
		else {
			removeTuple( entityKey, tupleOperation.getTupleContext() );
		}
	}

	private void executeBatchUpdate(Map<MongoCollection<Document>, BatchInsertionTask> inserts, List<Tuple> insertTuples,
			GroupedChangesToEntityOperation groupedOperation) {
		EntityKey entityKey = groupedOperation.getEntityKey();
		MongoCollection<Document> collection = getCollection( entityKey );
		Document insertStatement = null;
		Document updateStatement = new Document();
		WriteConcern writeConcern = null;

		final UpdateOptions updateOptions = new UpdateOptions().upsert( true );
		for ( Operation operation : groupedOperation.getOperations() ) {
			if ( operation instanceof InsertOrUpdateTupleOperation ) {
				InsertOrUpdateTupleOperation tupleOperation = (InsertOrUpdateTupleOperation) operation;
				Tuple tuple = tupleOperation.getTuplePointer().getTuple();
				MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) tuple.getSnapshot();
				writeConcern = mergeWriteConcern( writeConcern, getWriteConcern( tupleOperation.getTupleContext() ) );

				if ( SnapshotType.INSERT == tuple.getSnapshotType() ) {
					Document document = getCurrentDocument( snapshot, insertStatement, entityKey );
					insertStatement = objectForInsert( tuple, document );

					getOrCreateBatchInsertionTask( inserts, entityKey.getMetadata(), collection )
							.put( entityKey, insertStatement );
					insertTuples.add( tuple );
				}
				else {
					updateStatement = objectForUpdate( tuple, tupleOperation.getTupleContext(), updateStatement );
				}
			}
			else if ( operation instanceof InsertOrUpdateAssociationOperation ) {
				InsertOrUpdateAssociationOperation updateAssociationOperation = (InsertOrUpdateAssociationOperation) operation;
				Association association = updateAssociationOperation.getAssociation();
				AssociationKey associationKey = updateAssociationOperation.getAssociationKey();
				AssociationContext associationContext = updateAssociationOperation.getContext();
				AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( associationKey, associationContext );
				String collectionRole = associationKey.getMetadata().getCollectionRole();

				Object rows = getAssociationRows( association, associationKey, associationContext );
				Object toStore = associationKey.getMetadata().getAssociationType() == AssociationType.ONE_TO_ONE ? ( (List<?>) rows ).get( 0 ) : rows;

				if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
					writeConcern = mergeWriteConcern( writeConcern, getWriteConcern( associationContext ) );
					if ( insertStatement != null ) {
						// The association is updated in a new document
						MongoHelpers.setValue( insertStatement, collectionRole, rows );
					}
					else {
						// The association is updated on an existing document
						addSetToQuery( updateStatement, collectionRole, toStore );

						Document document = getDocument( association, associationContext );

						// The document might be null when deleting detached entities
						if ( document != null ) {
							MongoHelpers.setValue( document, associationKey.getMetadata().getCollectionRole(), toStore );
						}
					}
				}
				else {
					MongoDBAssociationSnapshot associationSnapshot = (MongoDBAssociationSnapshot) association.getSnapshot();
					MongoCollection<Document> associationCollection = getAssociationCollection( associationKey, storageStrategy );
					Document query = associationSnapshot.getQueryObject();
					Document update = new Document( "$set", new Document( ROWS_FIELDNAME, toStore ) );
					associationCollection.withWriteConcern( getWriteConcern( associationContext ) ).updateOne( query, update, updateOptions );
				}
			}
			else if ( operation instanceof RemoveAssociationOperation ) {
				if ( insertStatement != null ) {
					throw new IllegalStateException( "RemoveAssociationOperation not supported in the INSERT case" );
				}

				RemoveAssociationOperation removeAssociationOperation = (RemoveAssociationOperation) operation;
				AssociationKey associationKey = removeAssociationOperation.getAssociationKey();
				AssociationContext associationContext = removeAssociationOperation.getContext();

				AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( associationKey, associationContext );

				if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
					writeConcern = mergeWriteConcern( writeConcern, getWriteConcern( associationContext ) );
					String collectionRole = associationKey.getMetadata().getCollectionRole();

					if ( associationContext.getEntityTuplePointer().getTuple() != null ) {
						MongoHelpers.resetValue( ( (MongoDBTupleSnapshot) associationContext.getEntityTuplePointer().getTuple().getSnapshot() ).getDbObject(),
								collectionRole );
					}
					addUnsetToQuery( updateStatement, collectionRole );
				}
				else {
					MongoCollection<Document> associationCollection = getAssociationCollection( associationKey, storageStrategy ).withWriteConcern( getWriteConcern( associationContext ) );
					Document query = associationKeyToObject( associationKey, storageStrategy );
					DeleteResult result = associationCollection.deleteMany( query );
					long nAffected = -1;
					if ( result.wasAcknowledged() ) {
						nAffected = result.getDeletedCount();
					}
					log.removedAssociation( nAffected );
				}
			}
			else {
				throw new IllegalStateException( operation.getClass().getSimpleName() + " not supported here" );
			}
		}

		if ( updateStatement != null && !updateStatement.isEmpty() ) {
			collection. withWriteConcern( writeConcern ).updateOne( prepareIdObject( entityKey ), updateStatement , updateOptions );
		}
	}

	private Document getDocument(Association association, AssociationContext associationContext) {
		TuplePointer tuplePointer = associationContext.getEntityTuplePointer();
		if ( tuplePointer.getTuple() != null ) {
			Tuple tuple = tuplePointer.getTuple();
			MongoDBTupleSnapshot tupleSnapshot = (MongoDBTupleSnapshot) tuple.getSnapshot();
			return tupleSnapshot.getDbObject();
		}
		return null;
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return NoOpParameterMetadataBuilder.INSTANCE;
	}

	private static Document getCurrentDocument(MongoDBTupleSnapshot snapshot, Document insertStatement, EntityKey entityKey) {
		return insertStatement != null ? insertStatement : snapshot.getDbObject();
	}

	private static BatchInsertionTask getOrCreateBatchInsertionTask(Map<MongoCollection<Document>, BatchInsertionTask> inserts,
			EntityKeyMetadata entityKeyMetadata, MongoCollection<Document> collection) {
		BatchInsertionTask insertsForCollection = inserts.get( collection );

		if ( insertsForCollection == null ) {
			insertsForCollection = new BatchInsertionTask( entityKeyMetadata );
			inserts.put( collection, insertsForCollection );
		}

		return insertsForCollection;
	}

	private static void flushInserts(Map<MongoCollection<Document>, BatchInsertionTask> inserts) {
		for ( Map.Entry<MongoCollection<Document>, BatchInsertionTask> entry : inserts.entrySet() ) {
			MongoCollection<Document> collection = entry.getKey();
			if ( entry.getValue().isEmpty() ) {
				// has been emptied due to subsequent removals before flushes
				continue;
			}

			try {
				collection.insertMany( entry.getValue().getAll() );
			}
			catch ( DuplicateKeyException | MongoBulkWriteException dke ) {
				// This exception is used by MongoDB for all the unique indexes violation, not only the primary key
				// so we determine if it concerns the primary key by matching on the message
				if ( PRIMARY_KEY_CONSTRAINT_VIOLATION_MESSAGE.matcher( dke.getMessage() ).matches() ) {
					throw new TupleAlreadyExistsException( entry.getValue().getEntityKeyMetadata(), dke );
				}
				else {
					throw log.constraintViolationOnFlush( dke.getMessage(), dke );
				}
			}
		}
		inserts.clear();
	}

	private static WriteConcern getWriteConcern(TupleContext tupleContext) {
		return tupleContext.getTupleTypeContext().getOptionsContext().getUnique( WriteConcernOption.class );
	}

	private static WriteConcern getWriteConcern(AssociationContext associationContext) {
		return associationContext.getAssociationTypeContext().getOptionsContext().getUnique( WriteConcernOption.class );
	}

	/**
	 * As we merge several operations into one operation, we need to be sure the write concern applied to the aggregated
	 * operation respects all the requirements expressed for each separate operation.
	 *
	 * Thus, for each parameter of the write concern, we keep the stricter one for the resulting merged write concern.
	 */
	@SuppressWarnings("deprecation")
	private static WriteConcern mergeWriteConcern(WriteConcern original, WriteConcern writeConcern) {
		if ( original == null ) {
			return writeConcern;
		}
		else if ( writeConcern == null ) {
			return original;
		}
		else if ( original.equals( writeConcern ) ) {
			return original;
		}

		Object wObject;
		int wTimeoutMS;
		boolean fsync;
		Boolean journal;

		if ( original.getWObject() instanceof String ) {
			wObject = original.getWString();
		}
		else if ( writeConcern.getWObject() instanceof String ) {
			wObject = writeConcern.getWString();
		}
		else {
			wObject = Math.max( original.getW(), writeConcern.getW() );
		}

		wTimeoutMS = Math.min( original.getWtimeout(), writeConcern.getWtimeout() );

		fsync = original.getFsync() || writeConcern.getFsync();

		if ( original.getJournal() == null ) {
			journal = writeConcern.getJournal();
		}
		else if ( writeConcern.getJournal() == null ) {
			journal = original.getJournal();
		}
		else {
			journal = original.getJournal() || writeConcern.getJournal();
		}

		if ( wObject instanceof String ) {
			return new WriteConcern( (String) wObject, wTimeoutMS, fsync, journal );
		}
		else {
			return new WriteConcern( (int) wObject, wTimeoutMS, fsync, journal );
		}
	}

	private static ReadPreference getReadPreference(OperationContext operationContext) {
		return operationContext.getTupleTypeContext().getOptionsContext().getUnique( ReadPreferenceOption.class );
	}

	private static ReadPreference getReadPreference(AssociationContext associationContext) {
		return associationContext.getAssociationTypeContext().getOptionsContext().getUnique( ReadPreferenceOption.class );
	}

	private static class MongoDBAggregationOutput implements ClosableIterator<Tuple> {

		private final Iterator<Document> results;
		private final EntityKeyMetadata metadata;

		public MongoDBAggregationOutput(AggregateIterable<Document> output, EntityKeyMetadata metadata) {
			this.results = output.iterator();
			this.metadata = metadata;
		}

		@Override
		public boolean hasNext() {
			return results.hasNext();
		}

		@Override
		public Tuple next() {
			Document dbObject = results.next();
			return new Tuple( new MongoDBTupleSnapshot( dbObject, metadata ), SnapshotType.UPDATE );
		}

		@Override
		public void remove() {
			results.remove();
		}

		@Override
		public void close() {
			// Nothing to do
		}
	}

	private static class MongoDBTuplesSupplier implements TuplesSupplier {

		private final MongoCollection<Document> collection;
		private final EntityKeyMetadata entityKeyMetadata;

		public MongoDBTuplesSupplier(MongoCollection<Document> collection, EntityKeyMetadata entityKeyMetadata) {
			this.collection = collection;
			this.entityKeyMetadata = entityKeyMetadata;
		}

		@Override
		public ClosableIterator<Tuple> get(TransactionContext transactionContext) {
			return new MongoDBResultsCursor( collection.find().iterator(), entityKeyMetadata );
		}
	}

	private static class MongoDBResultsCursor implements ClosableIterator<Tuple> {

		private final MongoCursor<Document> cursor;
		private final EntityKeyMetadata metadata;

		public MongoDBResultsCursor(MongoCursor<Document> cursor, EntityKeyMetadata metadata) {
			this.cursor = cursor;
			this.metadata = metadata;
		}

		@Override
		public boolean hasNext() {
			return cursor.hasNext();
		}

		@Override
		public Tuple next() {
			Document dbObject = cursor.next();
			return new Tuple( new MongoDBTupleSnapshot( dbObject, metadata ), SnapshotType.UPDATE );
		}

		@Override
		public void remove() {
			cursor.remove();
		}

		@Override
		public void close() {
			cursor.close();
		}
	}

	private static class SingleTupleIterator implements ClosableIterator<Tuple> {
		private Document theOne;
		private final EntityKeyMetadata metadata;
		private final MongoCollection<Document> collection;

		public SingleTupleIterator(Document theOne, MongoCollection<Document> collection, EntityKeyMetadata metadata) {
			this.theOne = theOne;
			this.collection = collection;
			this.metadata = metadata;
		}

		@Override
		public boolean hasNext() {
			return theOne != null;
		}

		@Override
		public Tuple next() {
			if ( theOne == null ) {
				throw new NoSuchElementException(); // Seemingly a programming error if this line is ever reached.
			}

			Tuple t = new Tuple( new MongoDBTupleSnapshot( theOne, metadata ), SnapshotType.UPDATE );
			theOne = null;
			return t;
		}

		@Override
		public void remove() {
			if ( theOne == null ) {
				throw new IllegalStateException(); // Seemingly a programming error if this line is ever reached.
			}

			collection.deleteOne( theOne );
			theOne = null;
		}

		@Override
		public void close() {
			// Nothing to do.
		}
	}

	private static class BatchInsertionTask {

		private final EntityKeyMetadata entityKeyMetadata;
		private final Map<EntityKey, Document> inserts;

		public BatchInsertionTask(EntityKeyMetadata entityKeyMetadata) {
			this.entityKeyMetadata = entityKeyMetadata;
			this.inserts = new HashMap<EntityKey, Document>();
		}

		public EntityKeyMetadata getEntityKeyMetadata() {
			return entityKeyMetadata;
		}

		public List<Document> getAll() {
			return new ArrayList<Document>( inserts.values() );
		}

		public boolean containsKey(EntityKey entityKey) {
			return inserts.containsKey( entityKey );
		}

		public Document remove(EntityKey entityKey) {
			return inserts.remove( entityKey );
		}

		public void put(EntityKey entityKey, Document object) {
			inserts.put( entityKey, object );
		}

		public boolean isEmpty() {
			return inserts.isEmpty();
		}
	}
}
