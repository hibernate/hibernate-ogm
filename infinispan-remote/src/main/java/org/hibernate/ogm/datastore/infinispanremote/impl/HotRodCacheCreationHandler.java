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

/**
 * Creates {@link org.infinispan.client.hotrod.RemoteCache} if necessary,
 * apply a template to the cache, if required,
 * verifies the existence of any template
 *
 * @author Fabio Massimo Ercoli
 */
public class HotRodCacheCreationHandler {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final boolean createCachesEnabled;
	private final Map<String, String> cacheTemplatesByName;

	public HotRodCacheCreationHandler(boolean createCachesEnabled, String globalTemplate, Map<String, String> perCacheTemplate) {
		this.createCachesEnabled = createCachesEnabled;
		this.cacheTemplatesByName = perCacheTemplate;

		// applying globalTemplate to all caches where a template is not defined
		perCacheTemplate.forEach( ( cache, template ) -> {
			if ( template == null ) {
				perCacheTemplate.put( cache, globalTemplate );
			}
		} );
	}

	public void startAndValidateCaches(RemoteCacheManager hotrodClient) {
		Set<String> failedCacheNames = new HashSet<>();

		cacheTemplatesByName.entrySet().forEach( entry -> {
			startAndValidateCache( hotrodClient, entry.getKey(), entry.getValue(), failedCacheNames );
		} );

		if ( failedCacheNames.size() > 1 ) {
			throw log.expectedCachesNotDefined( failedCacheNames );
		}
		if ( failedCacheNames.size() == 1 ) {
			throw log.expectedCacheNotDefined( failedCacheNames.iterator().next() );
		}

	}

	protected void startAndValidateCache(RemoteCacheManager hotrodClient, String cacheName, String cacheTemplate, Set<String> failedCacheNames) {

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

		if ( cacheTemplate != null ) {
			// if a template is used, we check if exists
			RemoteCache<Object, Object> template = hotrodClient.getCache( cacheTemplate );
			if ( template == null ) {
				failedCacheNames.add( cacheTemplate );
				return;
			}
		}

		// finally create cache
		hotrodClient.administration().createCache( cacheName, cacheTemplate );
		cache = hotrodClient.getCache( cacheName );
		if ( cache == null ) {
			failedCacheNames.add( cacheName );
		}

	}

	public String getTemplate(String cacheName) {
		return cacheTemplatesByName.get( cacheName );
	}

	public Set<String> getMappedCacheNames() {
		return cacheTemplatesByName.keySet();
	}
}
