/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb;

import static org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot.SnapshotType.INSERT;
import static org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot.SnapshotType.UPDATE;
import static org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoHelpers.addEmptyAssociationField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.impl.AssociationStorageOption;
import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.AssociationStorageStrategy;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MassIndexingMongoDBTupleSnapshot;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBAssociationSnapshot;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoDBTupleSnapshot.SnapshotType;
import org.hibernate.ogm.datastore.mongodb.dialect.impl.MongoHelpers;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.impl.configuration.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.options.impl.AssociationDocumentStorageOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceOption;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl.MongoDBQueryDescriptorBuilder;
import org.hibernate.ogm.datastore.mongodb.query.parsing.nativequery.impl.NativeQueryParser;
import org.hibernate.ogm.datastore.mongodb.type.impl.ByteStringType;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.GridDialectOperationContext;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.dialect.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.Operation;
import org.hibernate.ogm.dialect.batch.OperationsQueue;
import org.hibernate.ogm.dialect.batch.RemoveAssociationOperation;
import org.hibernate.ogm.dialect.batch.RemoveTupleOperation;
import org.hibernate.ogm.dialect.batch.UpdateAssociationOperation;
import org.hibernate.ogm.dialect.batch.UpdateTupleOperation;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.IdGeneratorKeyMetadata.IdGeneratorType;
import org.hibernate.ogm.grid.Key;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.loader.nativeloader.BackendCustomQuery;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.query.NoOpParameterMetadataBuilder;
import org.hibernate.ogm.query.spi.ParameterMetadataBuilder;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.StringCalendarDateType;
import org.hibernate.ogm.util.ClosableIterator;
import org.hibernate.ogm.util.impl.CollectionHelper;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.parboiled.Parboiled;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.RecoveringParseRunner;
import org.parboiled.support.ParsingResult;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;

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
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 * @author Alan Fitton <alan at eth0.org.uk>
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MongoDBDialect implements BatchableGridDialect {

	public static final String ID_FIELDNAME = "_id";
	public static final String PROPERTY_SEPARATOR = ".";
	public static final String ROWS_FIELDNAME = "rows";
	public static final String TABLE_FIELDNAME = "table";
	public static final String ASSOCIATIONS_COLLECTION_PREFIX = "associations_";

	private static final Log log = LoggerFactory.getLogger();

	private static final String DEFAULT_TABLE_GENERATOR_VALUE_COLUMN_NAME = "sequence_value";

	private static final Pattern DOT_SEPARATOR_PATTERN = Pattern.compile( "\\." );
	private static final List<String> ROWS_FIELDNAME_LIST = Collections.singletonList( ROWS_FIELDNAME );

	private final MongoDBDatastoreProvider provider;
	private final DB currentDB;

	public MongoDBDialect(MongoDBDatastoreProvider provider) {
		this.provider = provider;
		this.currentDB = this.provider.getDatabase();
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		throw new UnsupportedOperationException( "The MongoDB GridDialect does not support locking" );
	}

	@Override
	public Tuple getTuple(EntityKey key, TupleContext tupleContext) {
		DBObject found = this.getObject( key, tupleContext );
		if ( found != null ) {
			return new Tuple( new MongoDBTupleSnapshot( found, key, UPDATE ) );
		}
		else if ( isInTheQueue( key, tupleContext ) ) {
			// The key has not been inserted in the db but it is in the queue
			return new Tuple( new MongoDBTupleSnapshot( prepareIdObject( key ), key, INSERT ) );
		}
		else {
			return null;
		}
	}

	private boolean isInTheQueue(EntityKey key, TupleContext tupleContext) {
		OperationsQueue queue = tupleContext.getOperationsQueue();
		return queue != null && queue.contains( key );
	}

	@Override
	public Tuple createTuple(EntityKey key, TupleContext tupleContext) {
		DBObject toSave = this.prepareIdObject( key );
		return new Tuple( new MongoDBTupleSnapshot( toSave, key, SnapshotType.INSERT ) );
	}

	/**
	 * Returns a {@link DBObject} representing the entity which embeds the specified association.
	 */
	private DBObject getEmbeddingEntity(AssociationKey key, AssociationContext associationContext) {
		ReadPreference readPreference = getReadPreference( associationContext );

		DBCollection collection = getCollection( key.getEntityKey() );
		DBObject searchObject = prepareIdObject( key.getEntityKey() );
		DBObject projection = getProjection( key, true );

		return collection.findOne( searchObject, projection, readPreference );
	}

	private DBObject getObject(EntityKey key, TupleContext tupleContext) {
		ReadPreference readPreference = getReadPreference( tupleContext );

		DBCollection collection = getCollection( key );
		DBObject searchObject = prepareIdObject( key );
		BasicDBObject projection = getProjection( tupleContext );

		return collection.findOne( searchObject, projection, readPreference );
	}

	private BasicDBObject getProjection(TupleContext tupleContext) {
		return getProjection( tupleContext.getSelectableColumns() );
	}

	/**
	 * Returns a projection object for specifying the fields to retrieve during a specific find operation.
	 */
	private BasicDBObject getProjection(List<String> fieldNames) {
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
	private BasicDBObject prepareIdObject(Key key) {
		return this.prepareIdObject( key.getColumnNames(), key.getColumnValues() );
	}

	private BasicDBObject prepareIdObject(String[] columnNames, Object[] columnValues) {
		BasicDBObject object;
		if ( columnNames.length == 1 ) {
			object = new BasicDBObject( ID_FIELDNAME, columnValues[0] );
		}
		else {
			object = new BasicDBObject();
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
			object.put( ID_FIELDNAME, idObject );
		}
		return object;
	}

	private DBCollection getCollection(String table) {
		return currentDB.getCollection( table );
	}

	private DBCollection getCollection(EntityKey key) {
		return getCollection( key.getTable() );
	}

	private DBCollection getAssociationCollection(AssociationKey key, AssociationStorageStrategy storageStrategy) {
		if ( storageStrategy == AssociationStorageStrategy.GLOBAL_COLLECTION ) {
			return getCollection( MongoDBConfiguration.DEFAULT_ASSOCIATION_STORE );
		}
		else {
			return getCollection( ASSOCIATIONS_COLLECTION_PREFIX + key.getTable() );
		}
	}

	private BasicDBObject getSubQuery(String operator, BasicDBObject query) {
		return query.get( operator ) != null ? (BasicDBObject) query.get( operator ) : new BasicDBObject();
	}

	private void addSubQuery(String operator, BasicDBObject query, String column, Object value) {
		BasicDBObject subQuery = this.getSubQuery( operator, query );
		query.append( operator, subQuery.append( column, value ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key, TupleContext tupleContext) {
		BasicDBObject idObject = this.prepareIdObject( key );
		DBObject updater = objectForUpdate( tuple, key, idObject );
		WriteConcern writeConcern = getWriteConcern( tupleContext );

		getCollection( key ).update( idObject, updater, true, false, writeConcern );
	}

	/**
	 * Creates a dbObject that can be pass to the mongoDB batch insert function
	 */
	private DBObject objectForInsert(Tuple tuple, DBObject dbObject) {
		MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) tuple.getSnapshot();
		for ( TupleOperation operation : tuple.getOperations() ) {
			String column = operation.getColumn();
			if ( notInIdField( snapshot, column ) ) {
				switch ( operation.getType() ) {
					case PUT_NULL:
					case PUT:
						dbObject.put( column, operation.getValue() );
						break;
					case REMOVE:
						dbObject.removeField( column );
						break;
					}
			}
		}
		return dbObject;
	}

	private DBObject objectForUpdate(Tuple tuple, EntityKey key, DBObject idObject) {
		MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) tuple.getSnapshot();

		BasicDBObject updater = new BasicDBObject();
		for ( TupleOperation operation : tuple.getOperations() ) {
			String column = operation.getColumn();
			if ( notInIdField( snapshot, column ) ) {
				switch ( operation.getType() ) {
				case PUT_NULL:
				case PUT:
					this.addSubQuery( "$set", updater, column, operation.getValue() );
					break;
				case REMOVE:
					this.addSubQuery( "$unset", updater, column, Integer.valueOf( 1 ) );
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

	private boolean notInIdField(MongoDBTupleSnapshot snapshot, String column) {
		return !column.equals( ID_FIELDNAME ) && !column.endsWith( PROPERTY_SEPARATOR + ID_FIELDNAME ) && !snapshot.columnInIdField( column );
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
		DBCollection collection = getCollection( key );
		DBObject toDelete = prepareIdObject( key );
		WriteConcern writeConcern = getWriteConcern( tupleContext );

		collection.remove( toDelete, writeConcern );
	}

	//not for embedded
	private DBObject findAssociation(AssociationKey key, AssociationContext associationContext, AssociationStorageStrategy storageStrategy) {
		ReadPreference readPreference = getReadPreference( associationContext );
		final DBObject associationKeyObject = associationKeyToObject( key, storageStrategy );

		return getAssociationCollection( key, storageStrategy ).findOne( associationKeyObject, getProjection( key, false ), readPreference );
	}

	private DBObject getProjection(AssociationKey key, boolean embedded) {
		if ( embedded ) {
			return getProjection( Collections.singletonList( key.getCollectionRole() ) );
		}
		else {
			return getProjection( ROWS_FIELDNAME_LIST );
		}
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );

		// We need to execute the previous operations first or it won't be able to find the key that should have
		// been created
		executeBatch( associationContext.getOperationsQueue() );
		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			DBObject entity = getEmbeddingEntity( key, associationContext );
			if ( getAssociationFieldOrNull( key, entity ) != null ) {
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

	private DBObject getAssociationFieldOrNull(AssociationKey key, DBObject entity) {
		String[] path = DOT_SEPARATOR_PATTERN.split( key.getCollectionRole() );
		DBObject field = entity;
		for ( String node : path ) {
			field = field != null ? (DBObject) field.get( node ) : null;
		}
		return field;
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );
		WriteConcern writeConcern = getWriteConcern( associationContext );

		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			DBObject entity = getEmbeddingEntity( key, associationContext );

			boolean insert = false;
			if ( entity == null ) {
				insert = true;
				entity = this.prepareIdObject( key.getEntityKey() );
			}

			if ( getAssociationFieldOrNull( key, entity ) == null ) {
				if ( insert ) {
					//adding assoc before insert
					addEmptyAssociationField( key, entity );
					getCollection( key.getEntityKey() ).insert( entity, writeConcern );
				}
				else {
					BasicDBObject updater = new BasicDBObject();
					this.addSubQuery( "$set", updater, key.getCollectionRole(),  Collections.EMPTY_LIST );
					//TODO use entity filter with only the ids
					this.getCollection( key.getEntityKey() ).update( entity, updater, true, false, writeConcern );
					//adding assoc after update because the query takes the whole object today
					addEmptyAssociationField( key, entity );
				}
			}
			return new Association( new MongoDBAssociationSnapshot( entity, key, storageStrategy ) );
		}
		DBCollection associations = getAssociationCollection( key, storageStrategy );
		DBObject assoc = associationKeyToObject( key, storageStrategy );

		assoc.put( ROWS_FIELDNAME, Collections.EMPTY_LIST );

		associations.insert( assoc, writeConcern );

		return new Association( new MongoDBAssociationSnapshot( assoc, key, storageStrategy ) );
	}

	/**
	 * Returns the rows of the given association as to be stored in the database. Elements of the returned list are
	 * either
	 * <ul>
	 * <li>plain values such as {@code String}s, {@code int}s etc. in case there is exactly one row key column which is
	 * not part of the association key (in this case we don't need to persist the key name as it can be restored from
	 * the association key upon loading) or</li>
	 * <li>{@code DBObject}s with keys/values for all row key columns which are not part of the association key</li>
	 * </ul>
	 */
	private List<?> getAssociationRows(Association association, AssociationKey key) {
		List<Object> rows = new ArrayList<Object>( association.getKeys().size() );

		for ( RowKey rowKey : association.getKeys() ) {
			rows.add( getAssociationRow( association.get( rowKey ), key ) );
		}

		return rows;
	}

	private Object getAssociationRow(Tuple row, AssociationKey associationKey) {
		String[] rowKeyColumnsToPersist = associationKey.getMetadata().getColumnsWithoutKeyColumns( row.getColumnNames() );

		// return value itself if there is only a single column to store
		if ( rowKeyColumnsToPersist.length == 1 ) {
			return row.get( rowKeyColumnsToPersist[0] );
		}
		// otherwise a DBObject with the row contents
		else {
			DBObject rowObject = new BasicDBObject( rowKeyColumnsToPersist.length );
			for ( String column : rowKeyColumnsToPersist ) {
				rowObject.put( column, row.get( column ) );
			}

			return rowObject;
		}
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		DBCollection collection;
		DBObject query;
		MongoDBAssociationSnapshot assocSnapshot = (MongoDBAssociationSnapshot) association.getSnapshot();
		String associationField;

		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );
		WriteConcern writeConcern = getWriteConcern( associationContext );

		// We need to execute the previous operations first or it won't be able to find the key that should have
		// been created
		executeBatch( associationContext.getOperationsQueue() );
		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			collection = this.getCollection( key.getEntityKey() );
			query = this.prepareIdObject( key.getEntityKey() );
			associationField = key.getCollectionRole();
		}
		else {
			collection = getAssociationCollection( key, storageStrategy );
			query = assocSnapshot.getQueryObject();
			associationField = ROWS_FIELDNAME;
		}

		List<?> rows = getAssociationRows( association, key );

		DBObject update = new BasicDBObject( "$set", new BasicDBObject( associationField, rows ) );

		collection.update( query, update, true, false, writeConcern );
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );
		WriteConcern writeConcern = getWriteConcern( associationContext );

		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			DBObject entity = this.prepareIdObject( key.getEntityKey() );
			if ( entity != null ) {
				BasicDBObject updater = new BasicDBObject();
				addSubQuery( "$unset", updater, key.getCollectionRole(), Integer.valueOf( 1 ) );
				getCollection( key.getEntityKey() ).update( entity, updater, true, false, writeConcern );
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
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple();
	}

	@Override
	public void nextValue(IdGeneratorKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		validateIdGeneratorKey( key );

		DBCollection currentCollection = getCollection( key.getTable() );
		DBObject query = this.prepareIdObject( key );
		//all columns should match to find the value

		String valueColumnName = getValueColumnName( key );

		BasicDBObject update = new BasicDBObject();
		//FIXME how to set the initialValue if the document is not present? It seems the inc value is used as initial new value
		Integer incrementObject = Integer.valueOf( increment );
		this.addSubQuery( "$inc", update, valueColumnName, incrementObject );
		DBObject result = currentCollection.findAndModify( query, null, null, false, update, false, true );
		Object idFromDB;
		idFromDB = result == null ? null : result.get( valueColumnName );
		if ( idFromDB == null ) {
			//not inserted yet so we need to add initial value to increment to have the right next value in the DB
			//FIXME that means there is a small hole as when there was not value in the DB, we do add initial value in a non atomic way
			BasicDBObject updateForInitial = new BasicDBObject();
			this.addSubQuery( "$inc", updateForInitial, valueColumnName, initialValue );
			currentCollection.findAndModify( query, null, null, false, updateForInitial, false, true );
			idFromDB = initialValue; //first time we ask this value
		}
		else {
			idFromDB = result.get( valueColumnName );
		}
		if ( idFromDB.getClass().equals( Integer.class ) || idFromDB.getClass().equals( Long.class ) ) {
			Number id = (Number) idFromDB;
			//idFromDB is the one used and the BD contains the next available value to use
			value.initialize( id.longValue() );
		}
		else {
			throw new HibernateException( "Cannot increment a non numeric field" );
		}
	}

	private String getValueColumnName(IdGeneratorKey key) {
		return key.getMetadata().getValueColumnName() != null ? key.getMetadata().getValueColumnName() : DEFAULT_TABLE_GENERATOR_VALUE_COLUMN_NAME;
	}

	private void validateIdGeneratorKey(IdGeneratorKey key) {
		if ( key.getMetadata().getType() != IdGeneratorType.TABLE ) {
			throw new HibernateException( "Unsupported generator type: " + key.getMetadata().getType() );
		}

		if ( !key.getColumnNames()[0].equals( ID_FIELDNAME ) ) {
			log.warnf( "Cannot use primary key column name '%s' for id generator, going to use '%s' instead", key.getColumnNames()[0], ID_FIELDNAME );
		}
	}


	@Override
	public boolean isStoredInEntityStructure(AssociationKey associationKey, AssociationContext associationContext) {
		return getAssociationStorageStrategy( associationKey, associationContext ) == AssociationStorageStrategy.IN_ENTITY;
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
		return null; // all other types handled as in hibernate-ogm-core
	}

	@Override
	public void forEachTuple(Consumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		DB db = provider.getDatabase();
		for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
			DBCollection collection = db.getCollection( entityKeyMetadata.getTable() );
			for ( DBObject dbObject : collection.find() ) {
				consumer.consume( new Tuple( new MassIndexingMongoDBTupleSnapshot( dbObject, entityKeyMetadata ) ) );
			}
		}
	}

	@Override
	public ClosableIterator<Tuple> executeBackendQuery(BackendCustomQuery customQuery, QueryParameters queryParameters) {
		MongoDBQueryDescriptor query = null;

		// query already given in DBObject-representation (created by JP-QL parser)
		if ( customQuery.getQueryObject() != null ) {
			query = (MongoDBQueryDescriptor) customQuery.getQueryObject();
		}
		// a string-based native query; need to create the DBObject from that
		else {
			// TODO OGM-414 This should actually be cached in the native query plan
			NativeQueryParser parser = Parboiled.createParser( NativeQueryParser.class );
			ParsingResult<MongoDBQueryDescriptorBuilder> parseResult =  new RecoveringParseRunner<MongoDBQueryDescriptorBuilder>( parser.Query() ).run( customQuery.getQueryString() );
			if (parseResult.hasErrors() ) {
				throw new IllegalArgumentException( "Unsupported native query: " + ErrorUtils.printParseErrors( parseResult.parseErrors ) );
			}

			query = parseResult.resultValue.build();
		}

		EntityKeyMetadata entityKeyMetadata = customQuery.getSingleEntityKeyMetadataOrNull();
		String collectionName = getCollectionName( customQuery, query, entityKeyMetadata );
		DBCollection collection = provider.getDatabase().getCollection( collectionName );

		switch( query.getOperation() ) {
			case FIND:
				return doFind( query, queryParameters, collection, entityKeyMetadata );
			case COUNT:
				return doCount( query, collection );
			default:
				throw new IllegalArgumentException( "Unexpected query operation: " + query );
		}
	}

	private ClosableIterator<Tuple> doFind(MongoDBQueryDescriptor query, QueryParameters queryParameters, DBCollection collection,
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

	private ClosableIterator<Tuple> doCount(MongoDBQueryDescriptor query, DBCollection collection) {
		long count = collection.count( query.getCriteria() );
		MapTupleSnapshot snapshot = new MapTupleSnapshot( Collections.<String, Object>singletonMap( "n", count ) );
		return CollectionHelper.newClosableIterator( Collections.singletonList( new Tuple( snapshot ) ) );
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
	private String getCollectionName(BackendCustomQuery customQuery, MongoDBQueryDescriptor queryDescriptor, EntityKeyMetadata entityKeyMetadata) {
		if ( queryDescriptor.getCollectionName() != null ) {
			return queryDescriptor.getCollectionName();
		}
		else if ( entityKeyMetadata != null ) {
			return entityKeyMetadata.getTable();
		}
		else {
			throw log.unableToDetermineCollectionName( customQuery.getQueryString() );
		}
	}

	private DBObject associationKeyToObject(AssociationKey key, AssociationStorageStrategy storageStrategy) {
		if ( storageStrategy == AssociationStorageStrategy.IN_ENTITY ) {
			throw new AssertionFailure( MongoHelpers.class.getName()
					+ ".associationKeyToObject should not be called for associations embedded in entity documents");
		}
		Object[] columnValues = key.getColumnValues();
		DBObject columns = new BasicDBObject( columnValues.length );

		int i = 0;
		for ( String name : key.getColumnNames() ) {
			columns.put( name, columnValues[i++] );
		}

		BasicDBObject idObject = new BasicDBObject( 1 );

		if ( storageStrategy == AssociationStorageStrategy.GLOBAL_COLLECTION ) {
			columns.put( MongoDBDialect.TABLE_FIELDNAME, key.getTable() );
		}
		idObject.put( MongoDBDialect.ID_FIELDNAME, columns );
		return idObject;
	}

	/**
	 * Returns the {@link AssociationStorageStrategy} effectively applying for the given association. If a setting is
	 * given via the option mechanism, that one will be taken, otherwise the default value as given via the
	 * corresponding configuration property is applied.
	 */
	private AssociationStorageStrategy getAssociationStorageStrategy(AssociationKey key, AssociationContext associationContext) {
		AssociationStorageType associationStorage = associationContext
				.getOptionsContext()
				.getUnique( AssociationStorageOption.class );

		AssociationDocumentType associationDocumentType = associationContext
				.getOptionsContext()
				.getUnique( AssociationDocumentStorageOption.class );

		return AssociationStorageStrategy.getInstance( key.getAssociationKind(), associationStorage, associationDocumentType );
	}

	/**
	 * When creating documents in batch, MongoDB doesn't allow the name of the fields to contain
	 * the characters '$' or '.'
	 *
	 * @return false if the {@link UpdateTupleOperation} affects fields containing invalid characters for the execution
	 * of operations in batch, true otherwise.
	 */
	private boolean columnNamesAllowBatchInsert(UpdateTupleOperation tupleOperation) {
		Set<String> columnNames = tupleOperation.getTuple().getColumnNames();
		for ( String column : columnNames ) {
			if ( column.contains( "." ) || column.contains( "$" ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void executeBatch(OperationsQueue queue) {
		if ( !queue.isClosed() ) {
			Operation operation = queue.poll();
			Map<DBCollection, BatchInsertionTask> inserts = new HashMap<DBCollection, BatchInsertionTask>();
			while ( operation != null ) {
				if ( operation instanceof UpdateTupleOperation ) {
					UpdateTupleOperation update = (UpdateTupleOperation) operation;
					executeBatchUpdate( inserts, update );
				}
				else if ( operation instanceof RemoveTupleOperation ) {
					RemoveTupleOperation tupleOp = (RemoveTupleOperation) operation;
					executeBatchRemove( inserts, tupleOp );
				}
				else if ( operation instanceof UpdateAssociationOperation ) {
					UpdateAssociationOperation update = (UpdateAssociationOperation) operation;
					updateAssociation( update.getAssociation(), update.getAssociationKey(), update.getContext() );
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

	private void executeBatchUpdate(Map<DBCollection, BatchInsertionTask> inserts, UpdateTupleOperation tupleOperation) {
		EntityKey entityKey = tupleOperation.getEntityKey();
		Tuple tuple = tupleOperation.getTuple();
		MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) tupleOperation.getTuple().getSnapshot();
		WriteConcern writeConcern = getWriteConcern( tupleOperation.getTupleContext() );

		if ( INSERT == snapshot.getOperationType() && columnNamesAllowBatchInsert( tupleOperation ) ) {
			prepareForInsert( inserts, snapshot, entityKey, tuple, writeConcern );
		}
		else {
			// Object already exists in the db or has invalid fields:
			updateTuple( tuple, entityKey, tupleOperation.getTupleContext() );
		}
	}

	@Override
	public ParameterMetadataBuilder getParameterMetadataBuilder() {
		return NoOpParameterMetadataBuilder.INSTANCE;
	}

	private void prepareForInsert(Map<DBCollection, BatchInsertionTask> inserts, MongoDBTupleSnapshot snapshot, EntityKey entityKey, Tuple tuple, WriteConcern writeConcern) {
		DBCollection collection = getCollection( entityKey );
		BatchInsertionTask batchInsertion = getOrCreateBatchInsertionTask( inserts, collection, writeConcern );
		DBObject document = getCurrentDocument( snapshot, batchInsertion, entityKey );
		DBObject newDocument = objectForInsert( tuple, document );
		inserts.get( collection ).put( entityKey, newDocument );
	}

	private DBObject getCurrentDocument(MongoDBTupleSnapshot snapshot, BatchInsertionTask batchInsert, EntityKey entityKey) {
		DBObject fromBatchInsertion = batchInsert.get( entityKey );
		return fromBatchInsertion != null ? fromBatchInsertion : snapshot.getDbObject();
	}

	private BatchInsertionTask getOrCreateBatchInsertionTask(Map<DBCollection, BatchInsertionTask> inserts, DBCollection collection, WriteConcern writeConcern) {
		BatchInsertionTask insertsForCollection = inserts.get( collection );

		if ( insertsForCollection == null ) {
			insertsForCollection = new BatchInsertionTask( writeConcern );
			inserts.put( collection, insertsForCollection );
		}

		return insertsForCollection;
	}

	private void flushInserts(Map<DBCollection, BatchInsertionTask> inserts) {
		for ( Map.Entry<DBCollection, BatchInsertionTask> entry : inserts.entrySet() ) {
			DBCollection collection = entry.getKey();
			collection.insert( entry.getValue().getAll(), entry.getValue().getWriteConcern() );
		}
		inserts.clear();
	}

	private WriteConcern getWriteConcern(GridDialectOperationContext operationContext) {
		return operationContext.getOptionsContext().getUnique( WriteConcernOption.class );
	}

	private ReadPreference getReadPreference(GridDialectOperationContext operationContext) {
		return operationContext.getOptionsContext().getUnique( ReadPreferenceOption.class );
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
			return new Tuple( new MassIndexingMongoDBTupleSnapshot( dbObject, metadata ) );
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

	private static class BatchInsertionTask {

		private final Map<EntityKey, DBObject> inserts;
		private final WriteConcern writeConcern;

		public BatchInsertionTask(WriteConcern writeConcern) {
			this.inserts = new HashMap<EntityKey, DBObject>();
			this.writeConcern = writeConcern;
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
	}
}
