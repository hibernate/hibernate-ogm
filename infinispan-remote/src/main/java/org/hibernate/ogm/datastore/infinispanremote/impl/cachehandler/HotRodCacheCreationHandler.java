/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.cachehandler;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.configuration.XMLStringConfiguration;

/**
 * Creates {@link org.infinispan.client.hotrod.RemoteCache} if necessary,
 * apply a configuration to the cache, if required,
 * verifies the existence of any configuration
 *
 * @author Fabio Massimo Ercoli
 */
public class HotRodCacheCreationHandler implements HotRodCacheHandler {

	private static final String OGM_BASIC_CONFIG =
			"<infinispan><cache-container>" +
					"	<distributed-cache-configuration name=\"%s\">" +
					"     <locking striping=\"false\" acquire-timeout=\"10000\" concurrency-level=\"50\" isolation=\"REPEATABLE_READ\"/>" +
					"     <transaction locking=\"PESSIMISTIC\" mode=\"%s\" />" +
					"     <expiration max-idle=\"-1\" />" +
					"     <indexing index=\"NONE\" />" +
					"     <state-transfer timeout=\"480000\" await-initial-transfer=\"true\" />" +
					"   </distributed-cache-configuration>" +
					"</cache-container></infinispan>";

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final Map<String, String> cacheConfigurations;

	private Set<String> failedCacheConfigurationNames = new HashSet<>();

	public HotRodCacheCreationHandler(String globalConfiguration, Map<String, String> perCacheConfiguration) {
		this.cacheConfigurations = perCacheConfiguration;

		// applying globalConfiguration to all caches where a configuration is not defined
		perCacheConfiguration.forEach( (cache, configuration) -> {
			if ( configuration == null ) {
				perCacheConfiguration.put( cache, globalConfiguration );
			}
		} );
	}

	@Override
	public void startAndValidateCaches(RemoteCacheManager hotrodClient) {
		cacheConfigurations.entrySet().forEach( entry -> {
			if ( entry.getValue() == null ) {
				startAndValidateCache( hotrodClient, entry.getKey() );
			}
			else {
				startAndValidateCache( hotrodClient, entry.getKey(), entry.getValue() );
			}
		} );

		if ( !failedCacheConfigurationNames.isEmpty() ) {
			throw log.expectedCacheConfiguratiosNotDefined( failedCacheConfigurationNames );
		}
	}

	protected void startAndValidateCache(RemoteCacheManager hotrodClient, String cacheName, String cacheConfiguration) {
		try {
			hotrodClient.administration()
					.getOrCreateCache( cacheName, cacheConfiguration );
		}
		catch (HotRodClientException ex) {
			failedCacheConfigurationNames.add( cacheConfiguration );
		}
	}

	protected void startAndValidateCache(RemoteCacheManager hotrodClient, String cacheName) {
		hotrodClient.administration()
				.getOrCreateCache( cacheName, getCacheConfiguration( cacheName ) );
	}

	private XMLStringConfiguration getCacheConfiguration(String cacheName) {
		return new XMLStringConfiguration( String.format( OGM_BASIC_CONFIG, cacheName, "NON_DURABLE_XA" ) );
	}

	@Override
	public String getConfiguration(String cacheName) {
		return cacheConfigurations.get( cacheName );
	}

	@Override
	public Set<String> getCaches() {
		return cacheConfigurations.keySet();
	}
}
