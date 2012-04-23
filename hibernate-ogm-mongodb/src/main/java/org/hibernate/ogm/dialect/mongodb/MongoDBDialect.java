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
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.impl.EmptyTupleSnapshot;
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
import org.hibernate.persister.entity.Lockable;
import org.hibernate.ogm.type.ByteStringType;
import org.hibernate.ogm.type.StringCalendarDateType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class MongoDBDialect implements GridDialect {

	private static final Log log = LoggerFactory.getLogger();
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
		return this.getObject( key ) != null ? new Tuple( new MongoDBTupleSnapshot( found ) ) : null;
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
		return getCollection( ASSOCIATIONS_COLLECTION_PREFIX + key.getTable() );
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
					this.addSubQuery( "$unset", updater, column, 1 );
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

	private DBObject findAssociation(AssociationKey key) {
		final DBObject associationKeyObject = MongoHelpers.associationKeyToObject( key );
		return this.getAssociationCollection( key ).findOne( associationKeyObject );
	}

	@Override
	public Association getAssociation(AssociationKey key) {
		final DBObject result = findAssociation( key );
		if ( result == null ) {
			return null;
		} else {
			return new Association( new MongoDBAssociationSnapshot( result ) );
		}
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		DBCollection associations = getAssociationCollection( key );
		DBObject assoc = MongoHelpers.associationKeyToObject( key );
		
		assoc.put( ROWS_FIELDNAME, Collections.EMPTY_LIST );
		associations.insert( assoc );
		
		return new Association( new MongoDBAssociationSnapshot( assoc ) );
	}

	private DBObject removeAssociationRowKey(MongoDBAssociationSnapshot snapshot, RowKey rowKey)
	{ 
		DBObject pull = new BasicDBObject( ROWS_FIELDNAME,  snapshot.getRowKeyDBObject( rowKey ) );
		return new BasicDBObject( "$pull", pull );
	}

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

	private static DBObject putAssociationRowKey(RowKey rowKey, Tuple value) {
		DBObject row = createBaseRowKey(rowKey);
		DBObject rowTupleMap = new BasicDBObject();
		
		for ( String valueKeyName : value.getColumnNames() )
			rowTupleMap.put( valueKeyName, value.get( valueKeyName ) );

		row.put( TUPLE_FIELDNAME, rowTupleMap );

		return new BasicDBObject( "$push", new BasicDBObject( ROWS_FIELDNAME, row ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		DBCollection collection = getAssociationCollection( key );
		MongoDBAssociationSnapshot assocSnapshot = (MongoDBAssociationSnapshot)association.getSnapshot();
		DBObject query = assocSnapshot.getQueryObject();
		
		for ( AssociationOperation action : association.getOperations() ) {
			RowKey rowKey = action.getKey();
			Tuple rowValue = action.getValue();

			DBObject update = null;

			switch ( action.getType() ) {
			case CLEAR:
				update = new BasicDBObject( "$set", new BasicDBObject(
						ROWS_FIELDNAME, Collections.EMPTY_LIST ) );
				break;
			case PUT_NULL:
			case PUT:
				update = putAssociationRowKey( rowKey, rowValue );
				break;
			case REMOVE:
				update = removeAssociationRowKey( assocSnapshot, rowKey );
				break;
			}

			if ( update != null )
				collection.update( query, update, true, false );
		}
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		DBCollection collection = getAssociationCollection( key );
		DBObject query = MongoHelpers.associationKeyToObject( key );
		
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
		this.addSubQuery( "$inc", update, SEQUENCE_VALUE, increment );
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
