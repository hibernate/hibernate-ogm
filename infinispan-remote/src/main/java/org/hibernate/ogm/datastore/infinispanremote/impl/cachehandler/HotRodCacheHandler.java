/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.cachehandler;

import java.util.Set;

import org.infinispan.client.hotrod.RemoteCacheManager;

/**
 * Handles {@link org.infinispan.client.hotrod.RemoteCache} at startup
 * Validating and optionally Creating them
 *
 * @author Fabio Massimo Ercoli
 */
public interface HotRodCacheHandler {

	void startAndValidateCaches(RemoteCacheManager hotrodClient);

	Set<String> getCaches();

	String getConfiguration(String cacheName);

}
