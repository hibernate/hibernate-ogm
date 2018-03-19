/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;

/**
 * Creates {@link org.infinispan.client.hotrod.RemoteCache} if necessary,
 * apply a configuration to the cache, if required,
 * verifies the existence of any configuration
 *
 * @author Fabio Massimo Ercoli
 */
public class HotRodCacheCreationHandler {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final boolean createCachesEnabled;
	private final Map<String, String> cacheConfigurationByName;

	public HotRodCacheCreationHandler(boolean createCachesEnabled, String globalConfiguration, Map<String, String> perCacheConfiguration) {
		this.createCachesEnabled = createCachesEnabled;
		this.cacheConfigurationByName = perCacheConfiguration;

		// applying globalConfiguration to all caches where a configuration is not defined
		perCacheConfiguration.forEach( ( cache, configuration ) -> {
			if ( configuration == null ) {
				perCacheConfiguration.put( cache, globalConfiguration );
			}
		} );
	}

	public void startAndValidateCaches(RemoteCacheManager hotrodClient) {
		Set<String> failedCacheNames = new HashSet<>();

		cacheConfigurationByName.entrySet().forEach( entry -> {
			startAndValidateCache( hotrodClient, entry.getKey(), entry.getValue(), failedCacheNames );
		} );

		if ( failedCacheNames.size() > 1 ) {
			throw log.expectedCachesNotDefined( failedCacheNames );
		}
		if ( failedCacheNames.size() == 1 ) {
			throw log.expectedCacheNotDefined( failedCacheNames.iterator().next() );
		}

	}

	protected void startAndValidateCache(RemoteCacheManager hotrodClient, String cacheName, String cacheConfiguration, Set<String> failedCacheNames) {

		RemoteCache<?, ?> cache = hotrodClient.getCache( cacheName );
		if ( cache != null ) {
			// cache already present
			return;
		}

		if ( !createCachesEnabled ) {
			// creation is not enabled so we have a failed cache
			failedCacheNames.add( cacheName );
			return;
		}

		// finally create cache
		try {
			hotrodClient.administration().createCache( cacheName, cacheConfiguration );
		}
		catch ( HotRodClientException ex ) {
			failedCacheNames.add( cacheConfiguration );
		}

	}

	public String getConfiguration(String cacheName) {
		return cacheConfigurationByName.get( cacheName );
	}

	public Set<String> getMappedCacheNames() {
		return cacheConfigurationByName.keySet();
	}
}
