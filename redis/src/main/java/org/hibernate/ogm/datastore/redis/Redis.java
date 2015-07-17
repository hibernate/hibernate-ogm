/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.redis.options.navigation.RedisGlobalContext;
import org.hibernate.ogm.datastore.redis.options.navigation.impl.RedisEntityContextImpl;
import org.hibernate.ogm.datastore.redis.options.navigation.impl.RedisGlobalContextImpl;
import org.hibernate.ogm.datastore.redis.options.navigation.impl.RedisPropertyContextImpl;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Allows to configure options specific to the Redis data store.
 *
 * @author Mark Paluch
 */
public class Redis implements DatastoreConfiguration<RedisGlobalContext> {

	/**
	 * Short name of this data store provider.
	 *
	 * @see OgmProperties#DATASTORE_PROVIDER
	 */
	public static final String DATASTORE_PROVIDER_NAME = "REDIS_EXPERIMENTAL";

	@Override
	public RedisGlobalContext getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext(
				RedisGlobalContextImpl.class,
				RedisEntityContextImpl.class,
				RedisPropertyContextImpl.class
		);
	}
}
