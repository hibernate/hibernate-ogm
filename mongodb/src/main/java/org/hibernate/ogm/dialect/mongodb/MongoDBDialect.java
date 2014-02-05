/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.dialect.mongodb;

import static org.hibernate.ogm.dialect.mongodb.MongoHelpers.addEmptyAssociationField;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.ogm.datastore.mongodb.AssociationDocumentType;
import org.hibernate.ogm.datastore.mongodb.impl.AssociationStorageStrategy;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.mongodb.impl.configuration.MongoDBConfiguration;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.datastore.spi.AssociationOperation;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.logging.mongodb.impl.Log;
import org.hibernate.ogm.logging.mongodb.impl.LoggerFactory;
import org.hibernate.ogm.massindex.batchindexing.Consumer;
import org.hibernate.ogm.options.generic.document.AssociationStorageType;
import org.hibernate.ogm.options.generic.document.impl.AssociationStorageOption;
import org.hibernate.ogm.options.mongodb.impl.AssociationDocumentStorageOption;
import org.hibernate.ogm.type.ByteStringType;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.StringCalendarDateType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

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
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 * @author Alan Fitton <alan at eth0.org.uk>
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MongoDBDialect implements GridDialect {

	public static final String ID_FIELDNAME = "_id";
	public static final String PROPERTY_SEPARATOR = ".";
	public static final String SEQUENCE_VALUE = "sequence_value";
	public static final String ROWS_FIELDNAME = "rows";
	public static final String TABLE_FIELDNAME = "table";
	public static final String ASSOCIATIONS_COLLECTION_PREFIX = "associations_";

	private static final Log log = LoggerFactory.getLogger();
	private static final Integer ONE = Integer.valueOf( 1 );
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
		return found != null ? new Tuple( new MongoDBTupleSnapshot( found, key ) ) : null;
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		DBObject toSave = this.prepareIdObject( key );
		return new Tuple( new MongoDBTupleSnapshot( toSave, key ) );
	}

	private DBObject getObjectAsEmbeddedAssociation(AssociationKey key) {
		DBCollection collection = this.getCollection( key.getEntityKey() );
		DBObject searchObject = this.prepareIdObject( key.getEntityKey() );
		DBObject restrictionObject = this.getSearchObject( key, true );
		return collection.findOne( searchObject, restrictionObject );
	}

	private DBObject getObject(EntityKey key, TupleContext tupleContext) {
		DBCollection collection = this.getCollection( key );
		DBObject searchObject = this.prepareIdObject( key );
		BasicDBObject restrictionObject = this.getSearchObject( tupleContext );
		return collection.findOne( searchObject, restrictionObject );
	}

	private BasicDBObject getSearchObject(TupleContext tupleContext) {
		return this.getSearchObject( tupleContext.getSelectableColumns() );
	}

	private BasicDBObject getSearchObject(List<String> selectedColumns) {
		BasicDBObject searchObject = new BasicDBObject();
		for ( String column : selectedColumns ) {
			searchObject.append( column, 1 );
		}
		return searchObject;
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
	private BasicDBObject prepareIdObject(EntityKey key) {
		return this.prepareIdObject( key.getColumnNames(), key.getColumnValues() );
	}

	private BasicDBObject prepareIdObject(RowKey key) {
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
		return this.currentDB.getCollection( table );
	}

	private DBCollection getCollection(EntityKey key) {
		return getCollection( key.getTable() );
	}

	private DBCollection getAssociationCollection(AssociationKey key, AssociationStorageStrategy storageStrategy) {
		if ( storageStrategy.isGlobalCollection() ) {
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
	public void updateTuple(Tuple tuple, EntityKey key) {
		MongoDBTupleSnapshot snapshot = (MongoDBTupleSnapshot) tuple.getSnapshot();

		BasicDBObject updater = new BasicDBObject();
		for ( TupleOperation operation : tuple.getOperations() ) {
			String column = operation.getColumn();
			if ( !column.equals( ID_FIELDNAME ) && !column.endsWith( PROPERTY_SEPARATOR + ID_FIELDNAME ) && !snapshot.columnInIdField(
					column
			) ) {
				switch ( operation.getType() ) {
				case PUT_NULL:
				case PUT:
					this.addSubQuery( "$set", updater, column, operation.getValue() );
					break;
				case REMOVE:
					this.addSubQuery( "$unset", updater, column, ONE );
					break;
				}
			}
		}
		BasicDBObject idObject = this.prepareIdObject( key );
		/*
		* Needed because in case of object with only an ID field
		* the "_id" won't be persisted properly.
		* With this adjustment, it will work like this:
		*	if the object (from snapshot) doesn't exist so create the one represented by updater
		*	so if at this moment the "_id" is not enforce properly an ObjectID will be created by the server instead
		*	of the custom id
		 */
		if ( updater.size() == 0 ) {
			updater = idObject;
		}
		this.getCollection( key ).update( idObject, updater, true, false );
	}

	@Override
	public void removeTuple(EntityKey key) {
		DBCollection collection = this.getCollection( key );
		DBObject toDelete = this.prepareIdObject( key );
		collection.remove( toDelete );
	}

	//not for embedded
	private DBObject findAssociation(AssociationKey key, AssociationStorageStrategy storageStrategy) {
		final DBObject associationKeyObject = associationKeyToObject( key, storageStrategy );

		return getAssociationCollection( key, storageStrategy ).findOne( associationKeyObject, getSearchObject( key, false ) );
	}

	private DBObject getSearchObject(AssociationKey key, boolean embedded) {
		if ( embedded ) {
			return getSearchObject( Collections.singletonList( key.getCollectionRole() ) );
		}
		else {
			return getSearchObject( ROWS_FIELDNAME_LIST );
		}
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );

		if ( storageStrategy.isEmbeddedInEntity() ) {
			DBObject entity = getObjectAsEmbeddedAssociation( key );
			if ( getAssociationFieldOrNull( key, entity ) != null ) {
				return new Association( new MongoDBAssociationSnapshot( entity, key, storageStrategy ) );
			}
			else {
				return null;
			}
		}
		final DBObject result = findAssociation( key, storageStrategy );
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

		if ( storageStrategy.isEmbeddedInEntity() ) {
			DBObject entity = getObjectAsEmbeddedAssociation( key );
			boolean insert = false;
			if ( entity == null ) {
				insert = true;
				entity = this.prepareIdObject( key.getEntityKey() );
			}
			if ( getAssociationFieldOrNull( key, entity ) == null ) {
				if ( insert ) {
					//adding assoc before insert
					addEmptyAssociationField( key, entity );
					getCollection( key.getEntityKey() ).insert( entity );
				}
				else {
					BasicDBObject updater = new BasicDBObject();
					this.addSubQuery( "$set", updater, key.getCollectionRole(),  Collections.EMPTY_LIST );
					//TODO use entity filter with only the ids
					this.getCollection( key.getEntityKey() ).update( entity, updater, true, false );
					//adding assoc after update because the query takes the whole object today
					addEmptyAssociationField( key, entity );
				}
			}
			return new Association( new MongoDBAssociationSnapshot( entity, key, storageStrategy ) );
		}
		DBCollection associations = getAssociationCollection( key, storageStrategy );
		DBObject assoc = associationKeyToObject( key, storageStrategy );

		assoc.put( ROWS_FIELDNAME, Collections.EMPTY_LIST );
		associations.insert( assoc );

		return new Association( new MongoDBAssociationSnapshot( assoc, key, storageStrategy ) );
	}

	private DBObject removeAssociationRowKey(MongoDBAssociationSnapshot snapshot, RowKey rowKey, String associationField) {
		DBObject pull = new BasicDBObject( associationField,  snapshot.getRowKeyDBObject( rowKey ) );
		return new BasicDBObject( "$pull", pull );
	}

	private DBObject putAssociationRowKey(Tuple value, String associationField, AssociationKey associationKey) {
		DBObject rowTupleMap = new BasicDBObject();
		for ( String valueKeyName : value.getColumnNames() ) {
			boolean add = true;
			//exclude columns from the associationKey as they can be guessed via metadata
			for ( String assocColumn : associationKey.getColumnNames() ) {
				if ( valueKeyName.equals( assocColumn ) ) {
					add = false;
					break;
				}
			}
			if (add) {
				rowTupleMap.put( valueKeyName, value.get( valueKeyName ) );
			}
		}
		DBObject row = rowTupleMap;
		return new BasicDBObject( "$push", new BasicDBObject( associationField, row ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key, AssociationContext associationContext) {
		DBCollection collection;
		DBObject query;
		MongoDBAssociationSnapshot assocSnapshot = (MongoDBAssociationSnapshot) association.getSnapshot();
		String associationField;

		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );

		if ( storageStrategy.isEmbeddedInEntity() ) {
			collection = this.getCollection( key.getEntityKey() );
			query = this.prepareIdObject( key.getEntityKey() );
			associationField = key.getCollectionRole();
		}
		else {
			collection = getAssociationCollection( key, storageStrategy );
			query = assocSnapshot.getQueryObject();
			associationField = ROWS_FIELDNAME;
		}

		for ( AssociationOperation action : association.getOperations() ) {
			RowKey rowKey = action.getKey();
			Tuple rowValue = action.getValue();

			DBObject update = null;

			switch ( action.getType() ) {
			case CLEAR:
				update = new BasicDBObject( "$set", new BasicDBObject( associationField, Collections.EMPTY_LIST ) );
				break;
			case PUT_NULL:
			case PUT:
				update = putAssociationRowKey( rowValue, associationField, key );
				break;
			case REMOVE:
				update = removeAssociationRowKey( assocSnapshot, rowKey, associationField );
				break;
			}

			if ( update != null ) {
				collection.update( query, update, true, false );
			}
		}
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
		AssociationStorageStrategy storageStrategy = getAssociationStorageStrategy( key, associationContext );

		if ( storageStrategy.isEmbeddedInEntity() ) {
			DBObject entity = this.prepareIdObject( key.getEntityKey() );
			if ( entity != null ) {
				BasicDBObject updater = new BasicDBObject();
				this.addSubQuery( "$unset", updater, key.getCollectionRole(), ONE );
				this.getCollection( key.getEntityKey() ).update( entity, updater, true, false );
			}
		}
		else {
			DBCollection collection = getAssociationCollection( key, storageStrategy );
			DBObject query = associationKeyToObject( key, storageStrategy );

			int nAffected = collection.remove( query ).getN();
			log.removedAssociation( nAffected );
		}
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple();
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		DBCollection currentCollection = this.currentDB.getCollection( key.getTable() );
		DBObject query = this.prepareIdObject( key );
		//all columns should match to find the value

		BasicDBObject update = new BasicDBObject();
		//FIXME should "value" be hardcoded?
		//FIXME how to set the initialValue if the document is not present? It seems the inc value is used as initial new value
		Integer incrementObject = increment == 1 ? ONE : Integer.valueOf( increment );
		this.addSubQuery( "$inc", update, SEQUENCE_VALUE, incrementObject );
		DBObject result = currentCollection.findAndModify( query, null, null, false, update, false, true );
		Object idFromDB;
		idFromDB = result == null ? null : result.get( SEQUENCE_VALUE );
		if ( idFromDB == null ) {
			//not inserted yet so we need to add initial value to increment to have the right next value in the DB
			//FIXME that means there is a small hole as when there was not value in the DB, we do add initial value in a non atomic way
			BasicDBObject updateForInitial = new BasicDBObject();
			this.addSubQuery( "$inc", updateForInitial, SEQUENCE_VALUE, initialValue );
			currentCollection.findAndModify( query, null, null, false, updateForInitial, false, true );
			idFromDB = initialValue; //first time we ask this value
		}
		else {
			idFromDB = result.get( SEQUENCE_VALUE );
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
	public Iterator<Tuple> executeBackendQuery(CustomQuery customQuery, EntityKeyMetadata[] metadatas) {
		BasicDBObject mongodbQuery = (BasicDBObject) com.mongodb.util.JSON.parse( customQuery.getSQL() );
		validate( metadatas );
		DBCollection collection = provider.getDatabase().getCollection( metadatas[0].getTable() );
		DBCursor cursor = collection.find( mongodbQuery );
		return new MongoDBResultsCursor( cursor, metadatas[0] );
	}

	private void validate(EntityKeyMetadata[] metadatas) {
		if ( metadatas.length != 1 ) {
			throw log.requireMetadatas();
		}
	}

	private DBObject associationKeyToObject(AssociationKey key, AssociationStorageStrategy storageStrategy) {
		if ( storageStrategy.isEmbeddedInEntity() ) {
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

		if ( storageStrategy.isGlobalCollection() ) {
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

		if ( associationStorage == null ) {
			associationStorage = provider.getAssociationStorage();
		}

		AssociationDocumentType associationDocumentType = associationContext
				.getOptionsContext()
				.getUnique( AssociationDocumentStorageOption.class );

		if ( associationDocumentType == null ) {
			associationDocumentType = provider.getAssociationDocumentStorage();
		}

		return AssociationStorageStrategy.getInstance( key.getAssociationKind(), associationStorage, associationDocumentType );
	}

	private static class MongoDBResultsCursor implements Iterator<Tuple>, Closeable {

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
		public void close() throws IOException {
			cursor.close();
		}

	}

}
