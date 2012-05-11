/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2012 Red Hat Inc. and/or its affiliates and other contributors
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

import java.util.Collections;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.impl.EmptyTupleSnapshot;
import org.hibernate.ogm.datastore.mongodb.Environment;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.AssociationOperation;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.logging.mongodb.impl.Log;
import org.hibernate.ogm.logging.mongodb.impl.LoggerFactory;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.StringCalendarDateType;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.ogm.type.ByteStringType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import static org.hibernate.ogm.dialect.mongodb.MongoHelpers.addEmptyAssociationField;
import static org.hibernate.ogm.dialect.mongodb.MongoHelpers.getAssociationFieldOrNull;
import static org.hibernate.ogm.dialect.mongodb.MongoHelpers.isEmbedded;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class MongoDBDialect implements GridDialect {

	private static final Log log = LoggerFactory.getLogger();
	private static final Integer ONE = Integer.valueOf( 1 );

	public static final String ID_FIELDNAME = "_id";
	public static final String SEQUENCE_VALUE = "sequence_value";
	public static final String ASSOCIATIONS_FIELDNAME = "associations";
	public static final String TUPLE_FIELDNAME = "tuple";
	public static final String COLUMNS_FIELDNAME = "columns";
	public static final String ROWS_FIELDNAME = "rows";
	public static final String TABLE_FIELDNAME = "table";
	public static final String ASSOCIATIONS_COLLECTION_PREFIX = "associations_";

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
	public Tuple getTuple(EntityKey key) {
		DBObject found = this.getObject( key );
		return found != null ? new Tuple( new MongoDBTupleSnapshot( found ) ) : null;
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		DBObject toSave = new BasicDBObject( ID_FIELDNAME, key.getColumnValues()[0] );
		return new Tuple( new MongoDBTupleSnapshot( toSave ) );

	}

	private DBObject getObject(EntityKey key) {
		DBCollection collection = this.getCollection( key );
		DBObject searchObject = new BasicDBObject( ID_FIELDNAME, key.getColumnValues()[0] );
		return collection.findOne( searchObject );
	}

	private DBCollection getCollection(String table) {
		return this.currentDB.getCollection( table );
	}

	private DBCollection getCollection(EntityKey key) {
		return getCollection( key.getTable() );
	}

	private DBCollection getAssociationCollection(AssociationKey key) {
		switch ( provider.getAssociationStorage() ) {
		case IN_ENTITY:
			return getCollection( key.getTable() );
		case GLOBAL_COLLECTION:
			return getCollection( Environment.MONGODB_DEFAULT_ASSOCIATION_STORE );
		case COLLECTION:
			return getCollection( ASSOCIATIONS_COLLECTION_PREFIX + key.getTable() );
		default:
			throw new AssertionFailure( "Unknown AssociationStorage: " + provider.getAssociationStorage() );
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
			if ( !column.equals( ID_FIELDNAME ) && !column.endsWith( "." + ID_FIELDNAME ) ) {
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
		this.getCollection( key ).update( snapshot.getDbObject(), updater, true, false );
	}

	@Override
	public void removeTuple(EntityKey key) {
		DBCollection collection = this.getCollection( key );
		DBObject toDelete = this.getObject( key );
		if ( toDelete != null ) {
			collection.remove( toDelete );
		}
		else {
			if ( log.isDebugEnabled() ) {
				log.debugf( "Unable to remove %1$s (object not found)", key.getColumnValues()[0] );
			}
		}
	}

	//not for embedded
	private DBObject findAssociation(AssociationKey key) {
		final DBObject associationKeyObject = MongoHelpers.associationKeyToObject( provider.getAssociationStorage(), key );
		return this.getAssociationCollection( key ).findOne( associationKeyObject );
	}

	@Override
	public Association getAssociation(AssociationKey key) {
		if ( isEmbedded( key ) ) {
			DBObject entity = getObject( key.getEntityKey() );
			if ( getAssociationFieldOrNull( key, entity ) != null ) {
				return new Association( new MongoDBAssociationSnapshot( entity, key ) );
			}
			else {
				return null;
			}
		}
		final DBObject result = findAssociation( key );
		if ( result == null ) {
			return null;
		} else {
			return new Association( new MongoDBAssociationSnapshot( result, key ) );
		}
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		if ( isEmbedded( key ) ) {
			DBObject entity = getObject( key.getEntityKey() );
			boolean insert = false;
			if ( entity == null ) {
				insert = true;
				entity = new BasicDBObject( ID_FIELDNAME, key.getEntityKey().getColumnValues()[0] );
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
			return new Association( new MongoDBAssociationSnapshot( entity, key ) );
		}
		DBCollection associations = getAssociationCollection( key );
		DBObject assoc = MongoHelpers.associationKeyToObject( provider.getAssociationStorage(), key );
		
		assoc.put( ROWS_FIELDNAME, Collections.EMPTY_LIST );
		associations.insert( assoc );
		
		return new Association( new MongoDBAssociationSnapshot( assoc, key ) );
	}

	private DBObject removeAssociationRowKey(MongoDBAssociationSnapshot snapshot, RowKey rowKey, String associationField) {
		DBObject pull = new BasicDBObject( associationField,  snapshot.getRowKeyDBObject( rowKey ) );
		return new BasicDBObject( "$pull", pull );
	}

	//non embedded only
	private static DBObject createBaseRowKey(RowKey rowKey) {
		DBObject row = new BasicDBObject();
		DBObject rowColumnMap = new BasicDBObject();
		Object[] columnValues = rowKey.getColumnValues();

		int i = 0;
		for ( String rowKeyColumnName : rowKey.getColumnNames() )
			rowColumnMap.put( rowKeyColumnName, columnValues[i++] );

		row.put( TABLE_FIELDNAME, rowKey.getTable() );
		row.put( COLUMNS_FIELDNAME, rowColumnMap );

		return row;
	}

	private DBObject putAssociationRowKey(RowKey rowKey, Tuple value, String associationField, AssociationKey associationKey) {
		DBObject rowTupleMap = new BasicDBObject();
		for ( String valueKeyName : value.getColumnNames() ) {
			rowTupleMap.put( valueKeyName, value.get( valueKeyName ) );
		}
		DBObject row;
		if ( isEmbedded( associationKey ) ) {
			row = rowTupleMap;
		}
		else {
			row = createBaseRowKey(rowKey);
			row.put( TUPLE_FIELDNAME, rowTupleMap );
		}
		return new BasicDBObject( "$push", new BasicDBObject( associationField, row ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		DBCollection collection;
		DBObject query;
		MongoDBAssociationSnapshot assocSnapshot = (MongoDBAssociationSnapshot)association.getSnapshot();
		String associationField;

		if ( isEmbedded( key ) ) {
			collection = this.getCollection( key.getEntityKey() );
			query = new BasicDBObject( ID_FIELDNAME, key.getEntityKey().getColumnValues()[0] );
			associationField = key.getCollectionRole();
		}
		else {
			collection = getAssociationCollection( key );
			query = assocSnapshot.getQueryObject();
			associationField = ROWS_FIELDNAME;
		}


		for ( AssociationOperation action : association.getOperations() ) {
			RowKey rowKey = action.getKey();
			Tuple rowValue = action.getValue();

			DBObject update = null;

			switch ( action.getType() ) {
			case CLEAR:
				update = new BasicDBObject( "$set", new BasicDBObject (associationField, Collections.EMPTY_LIST ) );
				break;
			case PUT_NULL:
			case PUT:
				update = putAssociationRowKey( rowKey, rowValue, associationField, key );
				break;
			case REMOVE:
				update = removeAssociationRowKey( assocSnapshot, rowKey, associationField );
				break;
			}

			if ( update != null )
				collection.update( query, update, true, false );
		}
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		if ( isEmbedded( key ) ) {
			DBObject entity = getObject( key.getEntityKey() );
			if ( entity != null ) {
				BasicDBObject updater = new BasicDBObject();
				this.addSubQuery( "$unset", updater, key.getCollectionRole(), ONE );
				this.getCollection( key.getEntityKey() ).update( entity, updater, true, false );
			}

		}
		DBCollection collection = getAssociationCollection( key );
		DBObject query = MongoHelpers.associationKeyToObject( provider.getAssociationStorage(), key );
		
		int nAffected = collection.remove( query ).getN();
		log.removedAssociation( nAffected );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple( EmptyTupleSnapshot.SINGLETON );
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		DBCollection currentCollection = this.currentDB.getCollection( key.getTable() );
		DBObject query = new BasicDBObject();
		int size = key.getColumnNames().length;
		//all columns should match to find the value
		for ( int index = 0; index < size; index++ ) {
			query.put( key.getColumnNames()[index], key.getColumnValues()[index] );
		}
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
}
