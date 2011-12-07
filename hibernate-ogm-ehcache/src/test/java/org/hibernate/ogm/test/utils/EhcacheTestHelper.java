package org.hibernate.ogm.test.utils;

import java.util.Map;

import net.sf.ehcache.Cache;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.grid.EntityKey;

import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ASSOCIATION_STORE;
import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ENTITY_STORE;

/**
 * @author Alex Snaps
 */
public class EhcacheTestHelper implements TestableGridDialect {
	@Override
	public int entityCacheSize(SessionFactory sessionFactory) {
		return getEntityCache( sessionFactory ).getSize();
	}

	@Override
	public int associationCacheSize(SessionFactory sessionFactory) {
		return getAssociationCache( sessionFactory ).getSize();
	}

	@Override
	public Map extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return (Map) getEntityCache( sessionFactory ).get( key ).getValue();
	}

	private static Cache getEntityCache(SessionFactory sessionFactory) {
		EhcacheDatastoreProvider castProvider = getProvider(sessionFactory);
		return castProvider.getCacheManager().getCache(ENTITY_STORE);
	}

	private static EhcacheDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ((SessionFactoryImplementor) sessionFactory).getServiceRegistry().getService(DatastoreProvider.class);
		if ( ! (EhcacheDatastoreProvider.class.isInstance(provider) ) ) {
			throw new RuntimeException("Not testing with Infinispan, cannot extract underlying cache");
		}
		return EhcacheDatastoreProvider.class.cast(provider);
	}

	private static Cache getAssociationCache(SessionFactory sessionFactory) {
		EhcacheDatastoreProvider castProvider = getProvider(sessionFactory);
		return castProvider.getCacheManager().getCache(ASSOCIATION_STORE);
	}

    /**
     * todo - we _are_ transactional. Turn this on. We could turn on XA or Local. Local will be faster. We will pick this up from the cache config.
     * @return
     */
	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}
}
