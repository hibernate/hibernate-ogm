package org.hibernate.ogm.dialect.ehcache;

import java.util.HashMap;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.datastore.impl.EmptyTupleSnapshot;
import org.hibernate.ogm.datastore.impl.MapBasedTupleSnapshot;
import org.hibernate.ogm.datastore.impl.MapHelpers;
import org.hibernate.ogm.datastore.mapbased.impl.MapAssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.DefaultDatastoreNames;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.persister.entity.Lockable;

/**
 * @author Alex Snaps
 */
public class EhcacheDialect implements GridDialect {
	
	EhcacheDatastoreProvider datastoreProvider;

	public EhcacheDialect(EhcacheDatastoreProvider datastoreProvider) {
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		if ( lockMode==LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
//		else if ( lockMode==LockMode.PESSIMISTIC_WRITE ) {
//			return new EhcachePessimisticWriteLockingStrategy( lockable, lockMode );
//		}
//		else if ( lockMode==LockMode.PESSIMISTIC_READ ) {
			//TODO find a more efficient pessimistic read
//			return new EhcachePessimisticWriteLockingStrategy( lockable, lockMode );
//		}
		else if ( lockMode==LockMode.OPTIMISTIC ) {
			return new OptimisticLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode==LockMode.OPTIMISTIC_FORCE_INCREMENT ) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		throw new UnsupportedOperationException( "LockMode " + lockMode + " is not supported by the Infinispan GridDialect" );
	}

	@Override
	public Tuple getTuple(EntityKey key) {
		final Cache entityCache = getEntityCache();
		final Element element = entityCache.get( key );
		if(element != null) {
			return new Tuple( new MapBasedTupleSnapshot( (Map<String, Object>) element.getValue() ) );
		} else {
			return null;
		}
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		final Cache entityCache = getEntityCache();
		final HashMap<String, Object> tuple = new HashMap<String, Object>();
		entityCache.put( new Element( key, tuple ) );
		return new Tuple( new MapBasedTupleSnapshot( tuple ) );
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		Map<String,Object> entityRecord = ( (MapBasedTupleSnapshot) tuple.getSnapshot() ).getMap();
		MapHelpers.applyTupleOpsOnMap( tuple, entityRecord );
	}

	@Override
	public void removeTuple(EntityKey key) {
		getEntityCache().remove( key );
	}

	@Override
	public Association getAssociation(AssociationKey key) {
		final Cache associationCache = getAssociationCache();
		final Element element = associationCache.get( key );
		if(element == null) {
			return null;
		} else {
			return new Association( new MapAssociationSnapshot( (Map) element.getValue() ) );
		}
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		final Cache associationCache = getAssociationCache();
		Map<RowKey, Map<String, Object>> association = new HashMap<RowKey, Map<String, Object>>(  );
		associationCache.put( new Element( key, association ) );
		return new Association( new MapAssociationSnapshot( association ) );
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		MapHelpers.updateAssociation( association, key );
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		getAssociationCache().remove( key );
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return new Tuple( EmptyTupleSnapshot.SINGLETON );
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		final Cache cache = getIdentifierCache();
		Element element = cache.get( key );
		if(element == null) {
			element = cache.putIfAbsent( new Element( key, initialValue ) );
		}
		int nextValue;
		if(element != null) {
			nextValue = (Integer) element.getValue() + increment;
			cache.replace( element, new Element( key, nextValue ) );
		} else {
			nextValue = initialValue;
		}
		value.initialize( nextValue );
	}

	private Cache getIdentifierCache() {
		return datastoreProvider.getCacheManager().getCache( DefaultDatastoreNames.IDENTIFIER_STORE );
	}

	private Cache getEntityCache() {
		return datastoreProvider.getCacheManager().getCache( DefaultDatastoreNames.ENTITY_STORE );
	}
	
	private Cache getAssociationCache() {
		return datastoreProvider.getCacheManager().getCache( DefaultDatastoreNames.ASSOCIATION_STORE );
	}
}
