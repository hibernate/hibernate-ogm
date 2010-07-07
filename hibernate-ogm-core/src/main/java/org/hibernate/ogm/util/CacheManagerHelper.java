package org.hibernate.ogm.util;

import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.manager.CacheManager;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.ogm.factory.CacheLifecycleManager;
import org.hibernate.ogm.grid.Key;

/**
 * Helper class to be refactored and abstracted from specific grid implementation
 * @author Emmanuel Bernard
 */
public class CacheManagerHelper {
	public static final String ENTITY_CACHE = "ENTITIES";

	public static CacheManager getCacheManager(SessionFactoryImplementor factory) {
		return getCacheLifecyleManager(factory).getCacheManager();
	}

	public static CacheLifecycleManager getCacheLifecyleManager(SessionFactoryImplementor factory) {
		final SessionFactoryObserver sessionFactoryObserver = factory.getFactoryObserver();
		if ( sessionFactoryObserver instanceof CacheLifecycleManager ) {
			return (CacheLifecycleManager) sessionFactoryObserver;
		}
		else {
			StringBuilder error = new StringBuilder("Cannot get CacheManager for OGM. ");
			if ( sessionFactoryObserver == null ) {
				error.append("SessionFactoryObserver not configured");
			}
			else {
				error.append("SessionFactoryObserver not of type " + CacheLifecycleManager.class);
			}
			throw new HibernateException( error.toString() );
		}
	}

	public static Cache<Key, Map<String, Object>> getEntityCache(SessionFactoryImplementor factory) {
		final CacheManager cacheManager = CacheManagerHelper.getCacheManager( factory );
		final Cache<Key, Map<String, Object>> cache = cacheManager.getCache( ENTITY_CACHE );
		return cache;
	}
}
