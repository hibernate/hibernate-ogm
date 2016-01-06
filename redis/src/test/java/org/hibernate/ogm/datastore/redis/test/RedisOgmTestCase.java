/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test;

import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.utils.OgmTestCase;

import com.lambdaworks.redis.api.sync.RedisCommands;

/**
 * Base class for OGM Redis tests providing access to the {@link RedisDatastoreProvider} and the {@link RedisCommands}.
 * @author Mark Paluch
 */
public abstract class RedisOgmTestCase extends OgmTestCase {

	protected RedisCommands<String, String> getConnection() {
		return getProvider().getConnection();
	}

	protected RedisDatastoreProvider getProvider() {
		return (RedisDatastoreProvider) sfi()
				.getServiceRegistry()
				.getService( DatastoreProvider.class );
	}
}
