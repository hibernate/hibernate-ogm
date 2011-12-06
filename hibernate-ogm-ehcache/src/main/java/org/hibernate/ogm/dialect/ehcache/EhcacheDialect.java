package org.hibernate.ogm.dialect.ehcache;

import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

import org.hibernate.LockMode;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.datastore.spi.Association;
import org.hibernate.ogm.datastore.spi.DefaultDatastoreNames;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
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
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Tuple getTuple(EntityKey key) {
		final Cache cache = datastoreProvider.getCacheManager().getCache( DefaultDatastoreNames.ENTITY_STORE );
		final Element element = cache.get( key );
		if(element != null) {
			return new Tuple( new TupleSnapshot() {
				@Override
				public Object get(String column) {
					return null;  //To change body of implemented methods use File | Settings | File Templates.
				}

				@Override
				public boolean isEmpty() {
					return false;  //To change body of implemented methods use File | Settings | File Templates.
				}

				@Override
				public Set<String> getColumnNames() {
					return null;  //To change body of implemented methods use File | Settings | File Templates.
				}
			} );
		} else {
			return null;
		}
	}

	@Override
	public Tuple createTuple(EntityKey key) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateTuple(Tuple tuple, EntityKey key) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void removeTuple(EntityKey key) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Association getAssociation(AssociationKey key) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Association createAssociation(AssociationKey key) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void updateAssociation(Association association, AssociationKey key) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void removeAssociation(AssociationKey key) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Tuple createTupleAssociation(AssociationKey associationKey, RowKey rowKey) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void nextValue(RowKey key, IntegralDataTypeHolder value, int increment, int initialValue) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
