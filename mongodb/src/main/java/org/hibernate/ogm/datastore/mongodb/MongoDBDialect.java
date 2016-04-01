/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb;

import static java.lang.Boolean.FALSE;
import static org.hibernate.ogm.datastore.document.impl.DotPatternMapHelpers.getColumnSharedPrefixOfAssociatedEntityLink;
import static org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot.SnapshotType.INSERT;
import static org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot.SnapshotType.UPDATE;
import static org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoHelpers.hasField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bson.types.ObjectId;
import org.hibernate.HibernateException;
import org.hibernate.annotations.common.AssertionFailure;
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
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot.SnapshotType;
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
import org.hibernate.ogm.datastore.mongodb.type.impl.ObjectIdGridType;
import org.hibernate.ogm.datastore.mongodb.type.impl.StringAsObjectIdGridType;
import org.hibernate.ogm.datastore.mongodb.type.impl.StringAsObjectIdType;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
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
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.dialect.spi.TupleContext;
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
import org.hibernate.ogm.model.spi.TupleOperation;
import org.hibernate.ogm.type.impl.ByteStringType;
import org.hibernate.ogm.type.impl.CharacterStringType;
import org.hibernate.ogm.type.impl.StringCalendarDateType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParsingResult;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.BulkWriteResult;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

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
 */
public class MongoDBDialect extends BaseGridDialect implements QueryableGridDialect<MongoDBQueryDescriptor>, BatchableGridDialect, IdentityColumnAwareGridDialect, MultigetGridDialect, OptimisticLockingAwareGridDialect {

	public static final String ID_FIELDNAME = "_id";
	public static final String PROPERTY_SEPARATOR = ".";
	public static final String ROWS_FIELDNAME = "rows";
	public static final String TABLE_FIELDNAME = "table";
	public static final String ASSOCIATIONS_COLLECTION_PREFIX = "associations_";

	private static final Log log = LoggerFactory.getLogger();

	private static final List<String> ROWS_FIELDNAME_LIST = Collections.singletonList( ROWS_FIELDNAME );

	private final MongoDBDatastoreProvider provider;
	private final DB currentDB;

	public MongoDBDialect(MongoDBDatastoreProvider provider) {
		this.provider = provider;
		this.currentDB = this.provider.getDatabase();
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		DBObject found = this.getObject( key, tupleContext );
		return createTuple( key, tupleContext, found );
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

		DBCursor cursor = this.getObjects( keys[0].getMetadata(), searchObjects, tupleContext );
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
	private static List<Tuple> tuplesResult(EntityKey[] keys, Object[] searchObjects, TupleContext tupleContext, DBCursor cursor) {
		// The list is initialized with null because some keys might not have a corresponding value in the cursor
		Tuple[] tuples = new Tuple[searchObjects.length];
		for ( DBObject dbObject : cursor ) {
			for ( int i = 0; i < searchObjects.length; i++ ) {
				if ( dbObject.get( ID_FIELDNAME ).equals( searchObjects[i] ) ) {
					tuples[i] = createTuple( keys[i], tupleContext, dbObject );
					// We assume there are no duplicated keys
					break;
				}
			}
		}
		return Arrays.asList( tuples );
	}

	private static Tuple createTuple(EntityKey key, TupleContext tupleContext, DBObject found) {
		if ( found != null ) {
			return new Tuple( new MongoDBTupleSnapshot( found, key.getMetadata(), UPDATE ) );
		}
		else if ( isInTheQueue( key, tupleContext ) ) {
			// The key has not been inserted in the db but it is in the queue
			return new Tuple( new MongoDBTupleSnapshot( prepareIdObject( key ), key.getMetadata(), INSERT ) );
		}
		else {
			return null;
		}
	}

	private static boolean isInTheQueue(EntityKey key, TupleContext tupleContext) {
		OperationsQueue queue = tupleContext.getOperationsQueue();
		return queue != null && queue.contains( key );
	}

	@Override
	public Tuple createTuple(EntityKeyMetadata entityKeyMetadata, TupleContext tupleContext) {
		return new Tuple( new MongoDBTupleSnapshot( new BasicDBObject(), entityKeyMetadata, SnapshotType.INSERT ) );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		DBObject toSave = prepareIdObject( key );
		return new Tuple( new MongoDBTupleSnapshot( toSave, key.getMetadata(), SnapshotType.INSERT ) );
	}

	/**
	 * Returns a {@link DBObject} representing the entity which embeds the specified association.
	 */
	private DBObject getEmbeddingEntity(AssociationKey key, AssociationContext associationContext) {
		DBObject embeddingEntityDocument = associationContext.getEntityTuple() != null ? ( (MongoDBTupleSnapshot) associationContext.getEntityTuple().getSnapshot() ).getDbObject() : null;

		if ( embeddingEntityDocument != null ) {
			return embeddingEntityDocument;
		}
		else {
			ReadPreference readPreference = getReadPreference( associationContext );

			DBCollection collection = getCollection( key.getEntityKey() );
			DBObject searchObject = prepareIdObject( key.getEntityKey() );
			DBObject projection = getProjection( key, true );

			return collection.findOne( searchObject, projection, readPreference );
		}
	}

	private DBObject getObject(EntityKey key, TupleContext tupleContext) {
		ReadPreference readPreference = getReadPreference( tupleContext );

		DBCollection collection = getCollection( key );
		DBObject searchObject = prepareIdObject( key );
		BasicDBObject projection = getProjection( tupleContext );

		return collection.findOne( searchObject, projection, readPreference );
	}

	private DBCursor getObjects(EntityKeyMetadata entityKeyMetadata, Object[] searchObjects, TupleContext tupleContext) {
		ReadPreference readPreference = getReadPreference( tupleContext );

		DBCollection collection = getCollection( entityKeyMetadata );
		collection.setReadPreference( readPreference );

		BasicDBObject projection = getProjection( tupleContext );

		DBObject query = new BasicDBObject();
		query.put( ID_FIELDNAME, new BasicDBObject( "$in", searchObjects ) );
		return collection.find( query, projection );
	}

	private static BasicDBObject getProjection(TupleContext tupleContext) {
		return getProjection( tupleContext.getSelectableColumns() );
	}

	/**
	 * Returns a projection object for specifying the fields to retrieve during a specific find operation.
	 */
	private static BasicDBObject getProjection(List<String> fieldNames) {
		BasicDBObject projection = new BasicDBObject( fieldNames.size() );
		for ( String column : fieldNames ) {
			projection.put( column, 1 );
		}

		return projection;
	}

	/**
	 * Create a DBObject which represents the _id field.
	 * In case of simple id objects the json representation will look like {_id: "theIdValue"}
	 * In case of composite id objects the json representation will look like {_id: {author: "Guillaume", title: "What this method is used for?"}}
	 *
	 * @param key
	 *
	 * @return the DBObject which represents the id field
	 */
	private static BasicDBObject prepareIdObject(EntityKey key) {
		return prepareIdObject( key.getColumnNames(), key.getColumnValues() );
	}

	private static BasicDBObject prepareIdObject(IdSourceKey key) {
		return prepareIdObject( key.getColumnNames(), key.getColumnValues() );
	}

	private static BasicDBObject prepareIdObject(String[] columnNames, Object[] columnValues) {
		return new BasicDBObject( ID_FIELDNAME, prepareIdObjectValue( columnNames, columnValues ) );
	}

	private static Object prepareIdObjectValue(String[] columnNames, Object[] columnValues) {
		if ( columnNames.length == 1 ) {
			return columnValues[0];
		}
		else {
			DBObject idObject = new BasicDBObject();
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

	private DBCollection getCollection(String table) {
		return currentDB.getCollection( table );
	}

	private DBCollection getCollection(EntityKey key) {
		return getCollection( key.getTable() );
	}

	private DBCollection getCollection(EntityKeyMetadata entityKeyMetadata) {
		return getCollection( entityKeyMetadata.getTable() );
	}

	private DBCollection getAssociationCollection(AssociationKey key, AssociationStorageStrategy storageStrategy) {
		if ( storageStrategy == AssociationStorageStrategy.GLOBAL_COLLECTION ) {
			return getCollection( MongoDBConfiguration.DEFAULT_ASSOCIATION_STORE );
		}
		else {
			return getCollection( ASSOCIATIONS_COLLECTION_PREFIX + key.getTable() );
		}
	}

	private static BasicDBObject getSubQuery(String operator, BasicDBObject query) {
		return query.get( operator ) != null ? (BasicDBObject) query.get( operator ) : new BasicDBObject();
	}

	private static void addSubQuery(String operator, BasicDBObject query, String column, Object value) {
		BasicDBObject subQuery = getSubQuery( operator, query );
		query.append( operator, subQuery.append( column, value ) );
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, Tuple tuple, TupleContext tupleContext) {
		BasicDBObject idObject = prepareIdObject( key );

		DBObject updater = objectForUpdate( tuple, idObject, tupleContext );
		WriteConcern writeConcern = getWriteConcern( tupleContext );

		try {
			getCollection( key ).update( idObject, updater, true, false, writeConcern );
		}
		catch ( DuplicateKeyException dke ) {
			throw new TupleAlreadyExistsException( key.getMetadata(), tuple, dke );
		}
	}

	@Override
	//TODO deal with dotted column names once this method is used for ALL / Dirty optimistic locking
	public boolean updateTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, Tuple tuple, TupleContext tupleContext) {
		BasicDBObject idObject = prepareIdObject( entityKey );

		for ( String versionColumn : oldLockState.getColumnNames() ) {
			idObject.put( versionColumn, oldLockState.get( versionColumn ) );
		}

		DBObject updater = objectForUpdate( tuple, idObject, tupleContext );
		DBObject doc = getCollection( entityKey ).findAndModify( idObject, updater );

		return doc != null;
	}

	@Override
	public void insertTuple(EntityKeyMetadata entityKeyMetadata, Tuple tuple, TupleContext tupleContext) {
		WriteConcern writeConcern = getWriteConcern( tupleContext );
		DBObject objectWithId = insertDBObject( entityKeyMetadata, tuple, writeConcern );
		String idColumnName = entityKeyMetadata.getColumnNames()[0];
		tuple.put( idColumnName, objectWithId.get( ID_FIELDNAME ) );
	}

	/*
	 * Insert the tuple and return an object containing the id in the field ID_FIELDNAME
	 */
	private DBObject insertDBObject(EntityKeyMetadata entityKeyMetadata, Tuple tuple, WriteConcern writeConcern) {
		DBObject dbObject = objectForInsert( tuple, ( (MongoDBTupleSnapshot) tuple.getSnapshot() ).getDbObject() );
		getCollection( entityKeyMetadata ).insert( dbObject, writeConcern );
		return dbObject;
	}

	/**
	 * Creates a dbObject that can be pass to the mongoDB batch insert function
	 */
	private static DBObject objectForInsert(Tuple tuple, DBObject dbObject) {
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

	private static DBObject objectForUpdate(Tuple tuple, DBObject idObject, TupleContext tupleContext) {
		MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) tuple.getSnapshot();
		EmbeddableStateFinder embeddableStateFinder = new EmbeddableStateFinder( tuple, tupleContext );
		Set<String> nullEmbeddables = new HashSet<String>();

		BasicDBObject updater = new BasicDBObject();
		for ( TupleOperation operation : tuple.getOperations() ) {
			String column = operation.getColumn();
			if ( notInIdField( snapshot, column ) ) {
				switch ( operation.getType() ) {
				case PUT:
					addSubQuery( "$set", updater, column, operation.getValue() );
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
							addSubQuery( "$unset", updater, nullEmbeddable, Integer.valueOf( 1 ) );
							nullEmbeddables.add( nullEmbeddable );
						}
					}
					else {
						// simply unset the column
						addSubQuery( "$unset", updater, column, Integer.valueOf( 1 ) );
					}
					break;
				}
			}
		}
		/*
		* Needed because in case of object with only an ID field
		* the "_id" won't be persisted properly.
		* With this adjustment, it will work like this:
		*	if the object (from snapshot) doesn't exist so create the one represented by updater
		*	so if at this moment the "_id" is not enforce properly an ObjectID will be created by the server instead
		*	of the custom id
		 */
		if ( updater.size() == 0 ) {
			return idObject;
		}
		return updater;
	}

	private static boolean notInIdField(MongoDBTupleSnapshot snapshot, String column) {
		return !column.equals( ID_FIELDNAME ) && !column.endsWith( PROPERTY_SEPARATOR + ID_FIELDNAME ) && !snapshot.isKeyColumn( column );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		DBCollection collection = getCollection( key );
		DBObject toDelete = prepareIdObject( key );
		WriteConcern writeConcern = getWriteConcern( tupleContext );

		collection.remove( toDelete, writeConcern );
	}

	@Override
	public boolean removeTupleWithOptimisticLock(EntityKey entityKey, Tuple oldLockState, TupleContext tupleContext) {
		DBObject toDelete = prepareIdObject( entityKey );

		for ( String versionColumn : oldLockState.getColumnNames() ) {
			toDelete.put( versionColumn, oldLockState.get( versionColumn ) );
		}

		DBCollection collection = getCollection( entityKey );
		DBObject deleted = collection.findAndRemove( toDelete );

		return deleted != null;
	}

	//not for embedded
	private DBObject findAssociation(AssociationKey key, AssociationContext associationContext, AssociationStorageStrategy storageStrategy) {
		ReadPreference readPreference = getReadPreference( associationContext );
		final DBObject associationKeyObject = associationKeyToObject( key, storageStrategy );

		return getAssociationCollection( key, storageStrategy ).findOne( associationKeyObject, getProjection( key, false ), readPreference );
	}

	private static DBObject getProjection(AssociationKey key, boolean embedded) {
		if ( embedded ) {
			return getProjection( Collections.singletonList( key.getMetadata().getCollectionRole() ) );
		}
		else {
			return getProjection( ROWS_FIELDNAME_LIST );
		}
	}

	private static boolean isInTheQueue(EntityKey key, AssociationContext associationContext) {
		OperationsQueue queue = associationContext.getOperationsQueue();
		return queue != null && queue.contains( key );
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );

		if ( isEmbeddedAssociation( key ) && isInTheQueue( key.getEntityKey(), associationContext ) ) {
			// The association is embedded and the owner of the association is in the insertion queue
			DBObject idObject = prepareIdObject( key.getEntityKey() );
			return new Association( new MongoDBAssociationSnapshot( idObject, key, storageStrategy ) );
		}

		// We need to execute the previous operations first or it won't be able to find the key that should have
		// been created
		executeBatch( associationContext.getOperationsQueue() );
		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			DBObject entity = getEmbeddingEntity( key, associationContext );

			if ( entity != null && hasField( entity, key.getMetadata().getCollectionRole() ) ) {
				return new Association( new MongoDBAssociationSnapshot( entity, key, storageStrategy ) );
			}
			else {
				return null;
			}
		}
		final DBObject result = findAssociation( key, associationContext, storageStrategy );
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

		DBObject document = storageStrategy == AssociationStorageStrategy.IN_ENTITY
				? getEmbeddingEntity( key, associationContext )
				: associationKeyToObject( key, storageStrategy );

		return new Association( new MongoDBAssociationSnapshot( document, key, storageStrategy ) );
	}

	/**
	 * Returns the rows of the given association as to be stored in the database. The return value is one of the
	 * following:
	 * <ul>
	 * <li>A list of plain values such as {@code String}s, {@code int}s etc. in case there is exactly one row key column
	 * which is not part of the association key (in this case we don't need to persist the key name as it can be
	 * restored from the association key upon loading) or</li>
	 * <li>A list of {@code DBObject}s with keys/values for all row key columns which are not part of the association
	 * key</li>
	 * <li>A {@link DBObject} with a key for each entry in case the given association has exactly one row key column
	 * which is of type {@code String} (e.g. a hash map) and {@link DocumentStoreProperties#MAP_STORAGE} is not set to
	 * {@link MapStorageType#AS_LIST}. The map values will either be plain values (in case it's single values) or
	 * another {@code DBObject}.
	 * </ul>
	 */
	private static Object getAssociationRows(Association association, AssociationKey key, AssociationContext associationContext) {
		boolean organizeByRowKey = DotPatternMapHelpers.organizeAssociationMapByRowKey( association, key, associationContext );

		// transform map entries such as ( addressType='home', address_id=123) into the more
		// natural ( { 'home'=123 }
		if ( organizeByRowKey ) {
			String rowKeyColumn = organizeByRowKey ? key.getMetadata().getRowKeyIndexColumnNames()[0] : null;
			DBObject rows = new BasicDBObject();

			for ( RowKey rowKey : association.getKeys() ) {
				DBObject row = (DBObject) getAssociationRow( association.get( rowKey ), key );

				String rowKeyValue = (String) row.removeField( rowKeyColumn );

				// if there is a single column on the value side left, unwrap it
				if ( row.keySet().size() == 1 ) {
					rows.put( rowKeyValue, row.toMap().values().iterator().next() );
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
		// otherwise a DBObject with the row contents
		else {
			// if the columns are only made of the embedded id columns, remove the embedded id property prefix
			// collectionrole: [ { id: { id1: "foo", id2: "bar" } } ] becomes collectionrole: [ { id1: "foo", id2: "bar" } ]
			String prefix = getColumnSharedPrefixOfAssociatedEntityLink( associationKey );

			DBObject rowObject = new BasicDBObject( rowKeyColumnsToPersist.length );
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
		DBCollection collection;
		DBObject query;
		MongoDBAssociationSnapshot assocSnapshot = (MongoDBAssociationSnapshot) association.getSnapshot();
		String associationField;

		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );
		WriteConcern writeConcern = getWriteConcern( associationContext );

		Object rows = getAssociationRows( association, key, associationContext );
		Object toStore = key.getMetadata().getAssociationType() == AssociationType.ONE_TO_ONE ? ( (List<?>) rows ).get( 0 ) : rows;

		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			collection = this.getCollection( key.getEntityKey() );
			query = prepareIdObject( key.getEntityKey() );
			associationField = key.getMetadata().getCollectionRole();

			//TODO would that fail if getCollectionRole has dots?
			( (MongoDBTupleSnapshot) associationContext.getEntityTuple().getSnapshot() ).getDbObject().put( key.getMetadata().getCollectionRole(), toStore );
		}
		else {
			collection = getAssociationCollection( key, storageStrategy );
			query = assocSnapshot.getQueryObject();
			associationField = ROWS_FIELDNAME;
		}

		DBObject update = new BasicDBObject( "$set", new BasicDBObject( associationField, toStore ) );

		collection.update( query, update, true, false, writeConcern );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );
		WriteConcern writeConcern = getWriteConcern( associationContext );

		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			DBObject entity = prepareIdObject( key.getEntityKey() );
			if ( entity != null ) {
				BasicDBObject updater = new BasicDBObject();
				addSubQuery( "$unset", updater, key.getMetadata().getCollectionRole(), Integer.valueOf( 1 ) );
				DBObject dbObject = getEmbeddingEntity( key, associationContext );
				if ( dbObject != null ) {
					dbObject.removeField( key.getMetadata().getCollectionRole() );
					getCollection( key.getEntityKey() ).update( entity, updater, true, false, writeConcern );
				}
			}
		}
		else {
			DBCollection collection = getAssociationCollection( key, storageStrategy );
			DBObject query = associationKeyToObject( key, storageStrategy );

			int nAffected = collection.remove( query, writeConcern ).getN();
			log.removedAssociation( nAffected );
		}
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		DBCollection currentCollection = getCollection( request.getKey().getTable() );
		DBObject query = prepareIdObject( request.getKey() );
		//all columns should match to find the value

		String valueColumnName = request.getKey().getMetadata().getValueColumnName();

		BasicDBObject update = new BasicDBObject();
		//FIXME how to set the initialValue if the document is not present? It seems the inc value is used as initial new value
		Integer incrementObject = Integer.valueOf( request.getIncrement() );
		addSubQuery( "$inc", update, valueColumnName, incrementObject );
		DBObject result = currentCollection.findAndModify( query, null, null, false, update, false, true );
		Object idFromDB;
		idFromDB = result == null ? null : result.get( valueColumnName );
		if ( idFromDB == null ) {
			//not inserted yet so we need to add initial value to increment to have the right next value in the DB
			//FIXME that means there is a small hole as when there was not value in the DB, we do add initial value in a non atomic way
			BasicDBObject updateForInitial = new BasicDBObject();
			addSubQuery( "$inc", updateForInitial, valueColumnName, request.getInitialValue() );
			currentCollection.findAndModify( query, null, null, false, updateForInitial, false, true );
			idFromDB = request.getInitialValue(); //first time we ask this value
		}
		else {
			idFromDB = result.get( valueColumnName );
		}
		if ( idFromDB.getClass().equals( Integer.class ) || idFromDB.getClass().equals( Long.class ) ) {
			Number id = (Number) idFromDB;
			//idFromDB is the one used and the BD contains the next available value to use
			return id;
		}
		else {
			throw new HibernateException( "Cannot increment a non numeric field" );
		}
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
		return null; // all other types handled as in hibernate-ogm-core
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		DB db = provider.getDatabase();
		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			DBCollection collection = db.getCollection( entityKeyMetadata.getTable() );
			for ( DBObject dbObject : collection.find() ) {
				consumer.consume( new Tuple( new MongoDBTupleSnapshot( dbObject, entityKeyMetadata, UPDATE ) ) );
			}
		}
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendQuery<MongoDBQueryDescriptor> backendQuery, QueryParameters queryParameters) {
		MongoDBQueryDescriptor queryDescriptor = backendQuery.getQuery();

		EntityKeyMetadata entityKeyMetadata = backendQuery.getSingleEntityKeyMetadataOrNull();
		String collectionName = getCollectionName( backendQuery, queryDescriptor, entityKeyMetadata );
		DBCollection collection = provider.getDatabase().getCollection( collectionName );

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
			case COUNT:
				return doCount( queryDescriptor, collection );
			case INSERT:
			case REMOVE:
			case UPDATE:
				throw log.updateQueryMustBeExecutedViaExecuteUpdate( queryDescriptor );
			default:
				throw new IllegalArgumentException( "Unexpected query operation: " + queryDescriptor );
		}
	}

	@Override
	public int executeBackendUpdateQuery(final BackendQuery<MongoDBQueryDescriptor> backendQuery, final QueryParameters queryParameters) {
		MongoDBQueryDescriptor queryDescriptor = backendQuery.getQuery();

		EntityKeyMetadata entityKeyMetadata = backendQuery.getSingleEntityKeyMetadataOrNull();
		String collectionName = getCollectionName( backendQuery, queryDescriptor, entityKeyMetadata );
		DBCollection collection = provider.getDatabase().getCollection( collectionName );

		if ( !queryParameters.getPositionalParameters().isEmpty() ) { // TODO Implement binding positional parameters.
			throw new UnsupportedOperationException("Positional parameters are not yet supported for MongoDB native queries.");
		}

		switch ( queryDescriptor.getOperation() ) {
			case INSERT:
				return doInsert( queryDescriptor, collection );
			case REMOVE:
				return doRemove( queryDescriptor, collection );
			case UPDATE:
				return doUpdate( queryDescriptor, collection );
			case FIND:
			case FINDONE:
			case FINDANDMODIFY:
			case AGGREGATE:
			case COUNT:
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
		if (parseResult.hasErrors() ) {
			throw new IllegalArgumentException( "Unsupported native query: " + ErrorUtils.printParseErrors( parseResult.parseErrors ) );
		}

		return parseResult.resultValue.build();
	}

	@Override
	public DuplicateInsertPreventionStrategy getDuplicateInsertPreventionStrategy(EntityKeyMetadata entityKeyMetadata) {
		return DuplicateInsertPreventionStrategy.NATIVE;
	}

	private static ClosableIterator<Tuple> doAggregate(MongoDBQueryDescriptor query, QueryParameters queryParameters, DBCollection collection, EntityKeyMetadata entityKeyMetadata) {
		List<DBObject> pipeline = new ArrayList<DBObject>();

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

		// apply firstRow/maxRows if present
		if ( queryParameters.getRowSelection().getFirstRow() != null ) {
			pipeline.add( stage( "$skip", queryParameters.getRowSelection().getFirstRow() ) );
		}

		if ( queryParameters.getRowSelection().getMaxRows() != null ) {
			pipeline.add( stage( "$limit", queryParameters.getRowSelection().getMaxRows() ) );
		}

		AggregationOutput output = collection.aggregate( pipeline );
		return new MongoDBAggregationOutput( output, entityKeyMetadata );
	}

	private static DBObject stage(String key, Object value) {
		DBObject stage = new BasicDBObject();
		stage.put( key, value );
		return stage;
	}

	private static ClosableIterator<Tuple> doFind(MongoDBQueryDescriptor query, QueryParameters queryParameters, DBCollection collection,
			EntityKeyMetadata entityKeyMetadata) {
		DBCursor cursor = collection.find( query.getCriteria(), query.getProjection() );
		if ( query.getOrderBy() != null ) {
			cursor.sort( query.getOrderBy() );
		}

		// apply firstRow/maxRows if present
		if ( queryParameters.getRowSelection().getFirstRow() != null ) {
			cursor.skip( queryParameters.getRowSelection().getFirstRow() );
		}

		if ( queryParameters.getRowSelection().getMaxRows() != null ) {
			cursor.limit( queryParameters.getRowSelection().getMaxRows() );
		}

		return new MongoDBResultsCursor( cursor, entityKeyMetadata );
	}

	private static ClosableIterator<Tuple> doFindOne(final MongoDBQueryDescriptor query, final DBCollection collection,
			final EntityKeyMetadata entityKeyMetadata) {

		final DBObject theOne = collection.findOne( query.getCriteria(), query.getProjection() );
		return new SingleTupleIterator(theOne, collection, entityKeyMetadata);
	}

	private static ClosableIterator<Tuple> doFindAndModify(final MongoDBQueryDescriptor queryDesc, final DBCollection collection,
			final EntityKeyMetadata entityKeyMetadata) {

		DBObject query = (DBObject) queryDesc.getCriteria().get( "query" );
		DBObject fields = (DBObject) queryDesc.getCriteria().get( "fields" );
		DBObject sort = (DBObject) queryDesc.getCriteria().get( "sort" );
		Boolean remove = (Boolean) queryDesc.getCriteria().get( "remove" );
		DBObject update = (DBObject) queryDesc.getCriteria().get( "update" );
		Boolean nevv = (Boolean) queryDesc.getCriteria().get( "new" );
		Boolean upsert = (Boolean) queryDesc.getCriteria().get( "upsert" );
		Boolean bypass = (Boolean) queryDesc.getCriteria().get( "bypassDocumentValidation" );
		DBObject o = (DBObject) queryDesc.getCriteria().get( "writeConcern" );
		WriteConcern wc = getWriteConcern( o );
		final DBObject theOne = collection.findAndModify( query, fields, sort, ( remove != null ? remove : false ),
				update, (nevv != null ? nevv : false), (upsert != null ? upsert : false), (bypass != null ? bypass : false),
				0, TimeUnit.MILLISECONDS, (wc != null ? wc : collection.getWriteConcern() ) );
		return new SingleTupleIterator( theOne, collection, entityKeyMetadata );
	}

	@SuppressWarnings("unchecked")
	private static int doInsert(final MongoDBQueryDescriptor queryDesc, final DBCollection collection) {
		DBObject insert = queryDesc.getUpdateOrInsert();
		DBObject options = queryDesc.getOptions();
		Boolean ordered = FALSE;
		WriteConcern wc = null;
		if ( options != null ) {
			ordered = (Boolean) options.get( "ordered" );
			ordered = ( ordered != null ) ? ordered : FALSE;
			DBObject o = (DBObject) options.get( "writeConcern" );
			wc = getWriteConcern( o );
		}

		// Need to use BulkWriteOperation here rather than collection.insert(..) because the WriteResult returned
		// by the latter returns 0 for getN() even if the insert was successful (which is bizarre, but that's the way it
		// is defined...)
		BulkWriteOperation bo = ( ordered ) ? collection.initializeOrderedBulkOperation() : collection.initializeUnorderedBulkOperation();
		if ( insert instanceof List<?> ) {
			for ( DBObject i : (List<DBObject>) insert ) {
				bo.insert( i );
			}
		}
		else {
			bo.insert( insert );
		}

		final BulkWriteResult result = bo.execute( ( wc != null ? wc : collection.getWriteConcern() ) );

		if ( result.isAcknowledged() ) {
			return result.getInsertedCount();
		}
		return -1; // Not sure if we should throw an exception instead?
	}

	private static int doRemove(final MongoDBQueryDescriptor queryDesc, final DBCollection collection) {
		DBObject query = queryDesc.getCriteria();
		DBObject options = queryDesc.getOptions();
		Boolean justOne = FALSE;
		WriteConcern wc = null;
		if ( options != null ) {
			justOne = (Boolean) options.get( "justOne" );
			justOne = ( justOne != null ) ? justOne : FALSE;
			if ( justOne ) { // IMPROVE See https://jira.mongodb.org/browse/JAVA-759
				throw new UnsupportedOperationException( "Using 'justOne' in a remove query is not yet supported." );
			}
			DBObject o = (DBObject) options.get( "writeConcern" );
			wc = getWriteConcern( o );
		}

		final WriteResult result = collection.remove( query, ( wc != null ? wc : collection.getWriteConcern() ) );
		if ( result.wasAcknowledged() ) {
			return result.getN();
		}
		return -1; // Not sure if we should throw an exception instead?
	}

	private static int doUpdate(final MongoDBQueryDescriptor queryDesc, final DBCollection collection) {
		DBObject query = queryDesc.getCriteria();
		DBObject update = queryDesc.getUpdateOrInsert();
		DBObject options = queryDesc.getOptions();
		Boolean upsert = FALSE;
		Boolean multi = FALSE;
		WriteConcern wc = null;
		if ( options != null ) {
			upsert = (Boolean) options.get( "upsert" );
			upsert = ( upsert != null ) ? upsert : FALSE;
			multi = (Boolean) options.get( "multi" );
			multi = ( multi != null ) ? multi : FALSE;
			DBObject o = (DBObject) options.get( "writeConcern" );
			wc = getWriteConcern( o );
		}

		final WriteResult result = collection.update( query, update, upsert, multi, ( wc != null ? wc : collection.getWriteConcern() ) );
		if ( result.wasAcknowledged() ) {
			// IMPROVE How could we return result.getUpsertedId() if it was an upsert, or isUpdateOfExisting()?
			// I see only a possibility by using javax.persistence.StoredProcedureQuery in the application
			// and then using getOutputParameterValue(String) to get additional result values.
			return result.getN();
		}
		return -1; // Not sure if we should throw an exception instead?
	}

	private static ClosableIterator<Tuple> doCount(MongoDBQueryDescriptor query, DBCollection collection) {
		long count = collection.count( query.getCriteria() );
		MapTupleSnapshot snapshot = new MapTupleSnapshot( Collections.<String, Object>singletonMap( "n", count ) );
		return CollectionHelper.newClosableIterator( Collections.singletonList( new Tuple( snapshot ) ) );
	}

	/**
	 * @param obj A JSON object representing a write concern.
	 * @return The parsed write concern or <code>null</code> if <code>obj</code> is <code>null</code>.
	 */
	private static WriteConcern getWriteConcern(DBObject obj) {
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

	private static DBObject associationKeyToObject(AssociationKey key, AssociationStorageStrategy storageStrategy) {
		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			throw new AssertionFailure( MongoHelpers.class.getName()
					+ ".associationKeyToObject should not be called for associations embedded in entity documents");
		}
		Object[] columnValues = key.getColumnValues();
		DBObject columns = new BasicDBObject( columnValues.length );

		// if the columns are only made of the embedded id columns, remove the embedded id property prefix
		// _id: [ { id: { id1: "foo", id2: "bar" } } ] becomes _id: [ { id1: "foo", id2: "bar" } ]
		String prefix = DocumentHelpers.getColumnSharedPrefix( key.getColumnNames() );
		prefix = prefix == null ? "" : prefix + ".";
		int i = 0;
		for ( String name : key.getColumnNames() ) {
			MongoHelpers.setValue( columns, name.substring( prefix.length() ), columnValues[i++] );
		}

		BasicDBObject idObject = new BasicDBObject( 1 );

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
			Map<DBCollection, BatchInsertionTask> inserts = new HashMap<DBCollection, BatchInsertionTask>();

			List<MongoDBTupleSnapshot> insertSnapshots = new ArrayList<MongoDBTupleSnapshot>();

			while ( operation != null ) {
				if ( operation instanceof InsertOrUpdateTupleOperation ) {
					InsertOrUpdateTupleOperation update = (InsertOrUpdateTupleOperation) operation;
					executeBatchUpdate( inserts, update );
					MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) update.getTuple().getSnapshot();
					if ( snapshot.getSnapshotType() == INSERT ) {
						insertSnapshots.add( snapshot );
					}
				}
				else if ( operation instanceof RemoveTupleOperation ) {
					RemoveTupleOperation tupleOp = (RemoveTupleOperation) operation;
					executeBatchRemove( inserts, tupleOp );
				}
				else if ( operation instanceof InsertOrUpdateAssociationOperation ) {
					InsertOrUpdateAssociationOperation update = (InsertOrUpdateAssociationOperation) operation;
					executeBatchUpdateAssociation( inserts, update );
				}
				else if ( operation instanceof RemoveAssociationOperation ) {
					RemoveAssociationOperation remove = (RemoveAssociationOperation) operation;
					removeAssociation( remove.getAssociationKey(), remove.getContext() );
				}
				else {
					throw new UnsupportedOperationException( "Operation not supported on MongoDB: " + operation.getClass().getName() );
				}
				operation = queue.poll();
			}
			flushInserts( inserts );

			for ( MongoDBTupleSnapshot insertSnapshot : insertSnapshots ) {
				insertSnapshot.setSnapshotType( UPDATE );
			}
			queue.close();
		}
	}

	private void executeBatchRemove(Map<DBCollection, BatchInsertionTask> inserts, RemoveTupleOperation tupleOperation) {
		EntityKey entityKey = tupleOperation.getEntityKey();
		DBCollection collection = getCollection( entityKey );
		BatchInsertionTask batchedInserts = inserts.get( collection );

		if ( batchedInserts != null && batchedInserts.containsKey( entityKey ) ) {
			batchedInserts.remove( entityKey );
		}
		else {
			removeTuple( entityKey, tupleOperation.getTupleContext() );
		}
	}

	private void executeBatchUpdate(Map<DBCollection, BatchInsertionTask> inserts, InsertOrUpdateTupleOperation tupleOperation) {
		EntityKey entityKey = tupleOperation.getEntityKey();
		Tuple tuple = tupleOperation.getTuple();
		MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) tupleOperation.getTuple().getSnapshot();
		WriteConcern writeConcern = getWriteConcern( tupleOperation.getTupleContext() );

		if ( INSERT == snapshot.getSnapshotType() ) {
			prepareForInsert( inserts, snapshot, entityKey, tuple, writeConcern );
		}
		else {
			// Object already exists in the db or has invalid fields:
			insertOrUpdateTuple( entityKey, tuple, tupleOperation.getTupleContext() );
		}
	}

	private void executeBatchUpdateAssociation(Map<DBCollection, BatchInsertionTask> inserts, InsertOrUpdateAssociationOperation updateOp) {
		AssociationKey associationKey = updateOp.getAssociationKey();
		if ( isEmbeddedAssociation( associationKey ) ) {
			DBCollection collection = getCollection( associationKey.getEntityKey() );
			BatchInsertionTask batchInserts = inserts.get( collection );
			if ( batchInserts != null && batchInserts.containsKey( associationKey.getEntityKey() ) ) {
				// The owner of the association is in the insertion queue,
				// we are going to update it with the collection of elements
				WriteConcern writeConcern = getWriteConcern( updateOp.getContext() );
				BatchInsertionTask insertTask = getOrCreateBatchInsertionTask( inserts, associationKey.getEntityKey().getMetadata(), collection, writeConcern );
				DBObject documentForInsertion = insertTask.get( associationKey.getEntityKey() );
				Object embeddedElements = getAssociationRows( updateOp.getAssociation(), updateOp.getAssociationKey(), updateOp.getContext() );
				String collectionRole = associationKey.getMetadata().getCollectionRole();
				MongoHelpers.setValue( documentForInsertion, collectionRole, embeddedElements );
			}
			else {
				insertOrUpdateAssociation( updateOp.getAssociationKey(), updateOp.getAssociation(), updateOp.getContext() );
			}
		}
		else {
			insertOrUpdateAssociation( updateOp.getAssociationKey(), updateOp.getAssociation(), updateOp.getContext() );
		}
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return NoOpParameterMetadataBuilder.INSTANCE;
	}

	private void prepareForInsert(Map<DBCollection, BatchInsertionTask> inserts, MongoDBTupleSnapshot snapshot, EntityKey entityKey, Tuple tuple, WriteConcern writeConcern) {
		DBCollection collection = getCollection( entityKey );
		BatchInsertionTask batchInsertion = getOrCreateBatchInsertionTask( inserts, entityKey.getMetadata(), collection, writeConcern );
		DBObject document = getCurrentDocument( snapshot, batchInsertion, entityKey );
		DBObject newDocument = objectForInsert( tuple, document );
		inserts.get( collection ).put( entityKey, newDocument );
	}

	private static DBObject getCurrentDocument(MongoDBTupleSnapshot snapshot, BatchInsertionTask batchInsert, EntityKey entityKey) {
		DBObject fromBatchInsertion = batchInsert.get( entityKey );
		return fromBatchInsertion != null ? fromBatchInsertion : snapshot.getDbObject();
	}

	private static BatchInsertionTask getOrCreateBatchInsertionTask(Map<DBCollection, BatchInsertionTask> inserts, EntityKeyMetadata entityKeyMetadata, DBCollection collection, WriteConcern writeConcern) {
		BatchInsertionTask insertsForCollection = inserts.get( collection );

		if ( insertsForCollection == null ) {
			insertsForCollection = new BatchInsertionTask( entityKeyMetadata, writeConcern );
			inserts.put( collection, insertsForCollection );
		}

		return insertsForCollection;
	}

	private static void flushInserts(Map<DBCollection, BatchInsertionTask> inserts) {
		for ( Map.Entry<DBCollection, BatchInsertionTask> entry : inserts.entrySet() ) {
			DBCollection collection = entry.getKey();
			if ( entry.getValue().isEmpty() ) {
				// has been emptied due to subsequent removals before flushes
				continue;
			}

			try {
				collection.insert( entry.getValue().getAll(), entry.getValue().getWriteConcern() );
			}
			catch ( DuplicateKeyException dke ) {
				throw new TupleAlreadyExistsException( entry.getValue().getEntityKeyMetadata(), null, dke );
			}
		}
		inserts.clear();
	}

	private static WriteConcern getWriteConcern(TupleContext tupleContext) {
		return tupleContext.getOptionsContext().getUnique( WriteConcernOption.class );
	}

	private static WriteConcern getWriteConcern(AssociationContext associationContext) {
		return associationContext.getAssociationTypeContext().getOptionsContext().getUnique( WriteConcernOption.class );
	}

	private static ReadPreference getReadPreference(TupleContext tupleContext) {
		return tupleContext.getOptionsContext().getUnique( ReadPreferenceOption.class );
	}

	private static ReadPreference getReadPreference(AssociationContext associationContext) {
		return associationContext.getAssociationTypeContext().getOptionsContext().getUnique( ReadPreferenceOption.class );
	}

	private static class MongoDBAggregationOutput implements ClosableIterator<Tuple> {

		private final Iterator<DBObject> results;
		private final EntityKeyMetadata metadata;

		public MongoDBAggregationOutput(AggregationOutput output, EntityKeyMetadata metadata) {
			this.results = output.results().iterator();
			this.metadata = metadata;
		}

		@Override
		public boolean hasNext() {
			return results.hasNext();
		}

		@Override
		public Tuple next() {
			DBObject dbObject = results.next();
			return new Tuple( new MongoDBTupleSnapshot( dbObject, metadata, UPDATE ) );
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

	private static class MongoDBResultsCursor implements ClosableIterator<Tuple> {

		private final DBCursor cursor;
		private final EntityKeyMetadata metadata;

		public MongoDBResultsCursor(DBCursor cursor, EntityKeyMetadata metadata) {
			this.cursor = cursor;
			this.metadata = metadata;
		}

		@Override
		public boolean hasNext() {
			return cursor.hasNext();
		}

		@Override
		public Tuple next() {
			DBObject dbObject = cursor.next();
			return new Tuple( new MongoDBTupleSnapshot( dbObject, metadata, UPDATE ) );
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
		private DBObject theOne;
		private final EntityKeyMetadata metadata;
		private final DBCollection collection;

		public SingleTupleIterator(DBObject theOne, DBCollection collection, EntityKeyMetadata metadata) {
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

			Tuple t = new Tuple( new MongoDBTupleSnapshot( theOne, metadata, UPDATE ) );
			theOne = null;
			return t;
		}

		@Override
		public void remove() {
			if ( theOne == null ) {
				throw new IllegalStateException(); // Seemingly a programming error if this line is ever reached.
			}

			collection.remove( theOne );
			theOne = null;
		}

		@Override
		public void close() {
			// Nothing to do.
		}
	}

	private static class BatchInsertionTask {

		private final EntityKeyMetadata entityKeyMetadata;
		private final Map<EntityKey, DBObject> inserts;
		private final WriteConcern writeConcern;

		public BatchInsertionTask(EntityKeyMetadata entityKeyMetadata, WriteConcern writeConcern) {
			this.entityKeyMetadata = entityKeyMetadata;
			this.inserts = new HashMap<EntityKey, DBObject>();
			this.writeConcern = writeConcern;
		}

		public EntityKeyMetadata getEntityKeyMetadata() {
			return entityKeyMetadata;
		}

		public List<DBObject> getAll() {
			return new ArrayList<DBObject>( inserts.values() );
		}

		public DBObject get(EntityKey entityKey) {
			return inserts.get( entityKey );

		}
		public boolean containsKey(EntityKey entityKey) {
			return inserts.containsKey( entityKey );
		}

		public DBObject remove(EntityKey entityKey) {
			return inserts.remove( entityKey );
		}

		public void put(EntityKey entityKey, DBObject object) {
			inserts.put( entityKey, object );
		}

		public WriteConcern getWriteConcern() {
			return writeConcern;
		}

		public boolean isEmpty() {
			return inserts.isEmpty();
		}
	}
}
