/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.cachehandler;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;

import org.infinispan.client.hotrod.RemoteCacheManager;

/**
 * Validates {@link org.infinispan.client.hotrod.RemoteCache}
 *
 * @author Fabio Massimo Ercoli
 */
public class HotRodCacheValidationHandler implements HotRodCacheHandler {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final Set<String> cacheNames;

	public HotRodCacheValidationHandler(Set<String> cacheNames) {
		this.cacheNames = cacheNames;
	}

	@Override
	public void startAndValidateCaches(RemoteCacheManager hotrodClient) {

		Set<String> failedCacheNames = cacheNames.stream().filter( cacheName ->
			hotrodClient.getCache( cacheName ) == null
		).collect( Collectors.toSet() );

		if ( !failedCacheNames.isEmpty() ) {
			throw log.expectedCachesNotDefined( failedCacheNames );
		}

	}

	@Override
	public Set<String> getCaches() {
		return cacheNames;
	}

	@Override
	public String getConfiguration(String cacheName) {
		// configuration does not make sense if we can't create new caches on remote data store
		return null;
	}
}
