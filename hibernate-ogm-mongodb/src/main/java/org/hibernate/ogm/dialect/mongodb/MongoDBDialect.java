/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.persister.entity.Lockable;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.Hash;

/**
 * @author Guillaume Scheibel
 */
public class MongoDBDialect implements GridDialect {

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

	/**
	 * Get a strategy instance which knows how to acquire a database-level lock
	 * of the specified mode for this dialect.
	 * 
	 * @param lockable
	 *            The persister for the entity to be locked.
	 * @param lockMode
	 *            The type of lock to be acquired.
	 * @return The appropriate locking strategy.
	 * @since 3.2
	 */
	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		if (lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT) {
			return new PessimisticForceIncrementLockingStrategy(lockable, lockMode);
		}
		// else if ( lockMode==LockMode.PESSIMISTIC_WRITE ) {
		// return new InfinispanPessimisticWriteLockingStrategy( lockable,
		// lockMode );
		// }
		// else if ( lockMode==LockMode.PESSIMISTIC_READ ) {
		// //TODO find a more efficient pessimistic read
		// return new InfinispanPessimisticWriteLockingStrategy( lockable,
		// lockMode );
		// }
		else if (lockMode == LockMode.OPTIMISTIC) {
			return new OptimisticLockingStrategy(lockable, lockMode);
		} else if (lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT) {
			return new OptimisticForceIncrementLockingStrategy(lockable, lockMode);
		}
		throw new UnsupportedOperationException("LockMode " + lockMode + " is not supported by the Infinispan GridDialect");
	}

	@Override
	public Tuple getTuple(EntityKey key) {
		DBObject found = this.getObject(key);
		return this.getObject(key) != null ? new Tuple(new MongoDBTupleSnapshot(found)) : null;
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		DBObject toSave = new BasicDBObject("_id", key.getId().toString());
		DBCollection collection = this.getCollection(key);
		collection.save(toSave);
		return new Tuple(new MongoDBTupleSnapshot(toSave));

	}

	private DBObject getObject(EntityKey key) {
		DBCollection collection = this.getCollection(key);
		DBObject searchObject = new BasicDBObject("_id", key.getId().toString());
		return collection.findOne(searchObject);
	}

	private DBCollection getCollection(EntityKey key) {
		return this.currentDB.getCollection(key.getTable());
	}

	public Map<String, Object> buildFieldGraph(String[] remainingSubFields, Object value, Map<String, Object> currentGraph) {
		if (remainingSubFields.length == 1) {
			currentGraph.put(remainingSubFields[0], value);
		} else {
			String[] subFields = Arrays.copyOfRange(remainingSubFields, 1, remainingSubFields.length);
			Map<String, Object> subGraph = null;
			if (currentGraph.containsKey(remainingSubFields[0])) {
				Object current = currentGraph.get(remainingSubFields[0]);
				if (!(current instanceof Map)) {
					Map<String, Object> subMap = new HashMap<String, Object>();
					subMap.put(subFields[0], current);
				} else {
					subGraph = (Map<String, Object>) current;
				}
			} else {
				subGraph = new HashMap<String, Object>();
			}
			currentGraph.put(remainingSubFields[0], this.buildFieldGraph(subFields, value, subGraph));
		}

		return currentGraph;
	}

	public Map<String, Object> buildFieldsMap(Tuple tuple) {
		Map<String, Object> fields = new HashMap<String, Object>();
		for (String column : tuple.getColumnNames()) {
			String[] fieldNames = column.split("\\.");
			this.buildFieldGraph(fieldNames, tuple.get(column), fields);
		}
		return fields;
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		DBObject toUpdate = this.getObject(key);
		toUpdate.putAll(this.buildFieldsMap(tuple));
		this.getCollection(key).save(toUpdate);
	}

	@Override
	public void removeTuple(EntityKey key) {
		DBCollection collection = this.getCollection(key);
		DBObject toDelete = this.getObject(key);
		if (toDelete != null) {
			collection.remove(toDelete);
		} else {
			throw new HibernateException("Unable to find " + key + " for removing");
		}
	}

	@Override
	public Association getAssociation(AssociationKey key) {
		throw new UnsupportedOperationException("getAssociation");
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		throw new UnsupportedOperationException("createAssociation");
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		throw new UnsupportedOperationException("updateAssociation");
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		throw new UnsupportedOperationException("removeAssociation");
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		throw new UnsupportedOperationException("createTupleAssociation");
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		// final AdvancedCache<RowKey, Object> identifierCache =
		// provider.getCache(IDENTIFIER_STORE).getAdvancedCache();
		// boolean done = false;
		// do {
		// //read value
		// //skip locking proposed by Sanne
		// Object valueFromDb = identifierCache.withFlags( Flag.SKIP_LOCKING
		// ).get( key );
		// if ( valueFromDb == null ) {
		// //if not there, insert initial value
		// value.initialize( initialValue );
		// //TODO should we use GridTypes here?
		// valueFromDb = new Long( value.makeValue().longValue() );
		// final Object oldValue = identifierCache.putIfAbsent( key, valueFromDb
		// );
		// //check in case somebody has inserted it behind our back
		// if ( oldValue != null ) {
		// value.initialize( ( (Number) oldValue ).longValue() );
		// valueFromDb = oldValue;
		// }
		// }
		// else {
		// //read the value from the table
		// value.initialize( ( ( Number ) valueFromDb ).longValue() );
		// }
		//
		// //update value
		// final IntegralDataTypeHolder updateValue = value.copy();
		// //increment value
		// updateValue.add( increment );
		// //TODO should we use GridTypes here?
		// final Object newValueFromDb = updateValue.makeValue().longValue();
		// done = identifierCache.replace( key, valueFromDb, newValueFromDb );
		// }
		// while ( !done );
		throw new UnsupportedOperationException("nextValue");
	}
}
