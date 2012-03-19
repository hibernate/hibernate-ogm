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

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleOperation;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.persister.entity.Lockable;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class MongoDBDialect implements GridDialect {
	private static final Log log = LoggerFactory.make();
	public static final String ID_FIELDNAME = "_id";
	private final MongoDBDatastoreProvider provider;
	private DB currentDB;

	public MongoDBDialect(MongoDBDatastoreProvider provider) {
		this.provider = provider;
		this.currentDB = this.provider.getDatabase();
	}

	public MongoDBDialect() {
		super();
		this.provider = null;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		throw new UnsupportedOperationException( "LockMode " + lockMode
				+ " is not supported by the MongoDB GridDialect" );
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

	private DBCollection getCollection(EntityKey key) {
		return this.currentDB.getCollection( key.getTable() );
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
			if ( !column.contains( ID_FIELDNAME ) ) {
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
			if ( log.isTraceEnabled() ) {
				log.tracef( "Unable to remove %1$s (object not found)", key.getColumnValues()[0] );
			}
		}
	}

	@Override
	public Association getAssociation(AssociationKey key) {
		throw new UnsupportedOperationException( "getAssociation is not supported by the MongoDB GridDialect" );
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		throw new UnsupportedOperationException( "createAssociation is not supported by the MongoDB GridDialect" );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		throw new UnsupportedOperationException( "updateAssociation is not supported by the MongoDB GridDialect" );
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		throw new UnsupportedOperationException( "removeAssociation is not supported by the MongoDB GridDialect" );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		throw new UnsupportedOperationException( "createTupleAssociation is not supported by the MongoDB GridDialect" );
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		throw new UnsupportedOperationException( "nextValue is not supported by the MongoDB GridDialect" );
	}
}
