package org.hibernate.ogm.datastore.mapbased.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.persistence.PessimisticLockException;

import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * This is an example a DatastoreProvider, implementing only the basic interface needed by Hibernate OGM.
 *
 * It does not support transactions, nor clustering nor it has monitoring or capabilities to offload the
 * contents to other storage. Most important, it must be considered that different sessions won't be isolated
 * unless they avoid flushing.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class MapBasedDatastoreProvider implements DatastoreProvider, Startable, Stoppable {

	private static final Log log = LoggerFactory.make();

	private final ConcurrentMap<EntityKey,Map<String, Object>> entitiesKeyValueStorage = new ConcurrentHashMap<EntityKey,Map<String, Object>>();
	private final ConcurrentMap<AssociationKey, Map<RowKey, Map<String, Object>>> associationsKeyValueStorage = new ConcurrentHashMap<AssociationKey, Map<RowKey, Map<String, Object>>>();
	private final ConcurrentMap<RowKey, AtomicInteger> sequencesStorage = new ConcurrentHashMap<RowKey, AtomicInteger>();
	private final ConcurrentMap<Object,ReadWriteLock> dataLocks = new ConcurrentHashMap();

	/**
	 * This simplistic data store only supports thread-bound transactions:
	 */
	private final ThreadLocal<Set<Lock>> acquiredLocksPerThread = new ThreadLocal<Set<Lock>>() {
		@Override protected Set<Lock> initialValue() {
			return new HashSet<Lock>();
		}
	};

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return HashMapDialect.class;
	}

	@Override
	public void stop() {
		entitiesKeyValueStorage.clear();
		log.debug( "Stopped and cleared MapBasedDatastoreProvider" );
	}

	@Override
	public void start() {
		log.debug( "MapBasedDatastoreProvider started" );
	}

	/**
	 * Acquires a write lock on a specific key.
	 * @param key The key to lock
	 * @param timeout in milliseconds; -1 means wait indefinitely, 0 means no wait.
	 */
	public void writeLock(EntityKey key, int timeout) {
		ReadWriteLock lock = getLock( key );
		Lock writeLock = lock.writeLock();
		acquireLock( key, timeout, writeLock );
	}

	/**
	 * Acquires a read lock on a specific key.
	 * @param key The key to lock
	 * @param timeout in milliseconds; -1 means wait indefinitely, 0 means no wait.
	 */
	public void readLock(EntityKey key, int timeout) {
		ReadWriteLock lock = getLock( key );
		Lock readLock = lock.readLock();
		acquireLock( key, timeout, readLock );
	}

	private ReadWriteLock getLock(EntityKey key) {
		ReadWriteLock newLock = new ReentrantReadWriteLock();
		ReadWriteLock previous = dataLocks.putIfAbsent( key, newLock );
		return previous != null ? previous : newLock;
	}

	private void acquireLock(EntityKey key, int timeout, Lock writeLock) {
		try {
			if ( timeout == -1 ) {
				writeLock.lockInterruptibly();
			}
			else if ( timeout == 0 ) {
				boolean locked = writeLock.tryLock();
				if ( ! locked ) {
					throw new PessimisticLockException( "lock on key " + key + " was not available" );
				}
			}
			else {
				writeLock.tryLock( timeout, TimeUnit.MILLISECONDS );
			}
		}
		catch ( InterruptedException e ) {
			throw new PessimisticLockException( "timed out waiting for lock on key " + key, e );
		}
		acquiredLocksPerThread.get().add( writeLock );
	}

	public void putEntity(EntityKey key, Map<String, Object> tuple) {
		entitiesKeyValueStorage.put( key, tuple );
	}

	public Map<String, Object> getEntityTuple(EntityKey key) {
		return entitiesKeyValueStorage.get( key );
	}

	public void removeEntityTuple(EntityKey key) {
		entitiesKeyValueStorage.remove( key );
	}

	public void putAssociation(AssociationKey key, Map<RowKey, Map<String, Object>> associationMap) {
		associationsKeyValueStorage.put( key, associationMap );
	}

	public Map<RowKey, Map<String, Object>> getAssociation(AssociationKey key) {
		return associationsKeyValueStorage.get( key );
	}

	public void removeAssociation(AssociationKey key) {
		associationsKeyValueStorage.remove( key );
	}

	public int getSharedAtomicInteger(RowKey key, int initialValue, int increment) {
		AtomicInteger valueProposal = new AtomicInteger( initialValue );
		AtomicInteger previous = sequencesStorage.putIfAbsent( key, valueProposal );
		return previous == null ? initialValue : previous.addAndGet( increment );
	}

	/**
	 * Meant to execute assertions in tests only
	 * @return a read-only view of the map containing the entities
	 */
	public Map<EntityKey,Map<String, Object>> getEntityMap() {
		return Collections.unmodifiableMap( entitiesKeyValueStorage );
	}

	/**
	 * Meant to execute assertions in tests only
	 * @return a read-only view of the map containing the relations between entities
	 */
	public Map<AssociationKey, Map<RowKey, Map<String, Object>>> getAssociationsMap() {
		return Collections.unmodifiableMap( associationsKeyValueStorage );
	}

}
