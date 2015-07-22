/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis;

import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.keyvalue.cfg.KeyValueStoreProperties;

/**
 * Properties for configuring the Redis datastore via {@code persistence.xml} or {@link OgmConfiguration}.
 *
 * @author Mark Paluch
 */
public final class RedisProperties implements KeyValueStoreProperties {

	/**
	 * The timeout used at the connection to the Redis instance. This value is set in milliseconds. Defaults to 5000.
	 */
	public static final String TIMEOUT = "hibernate.ogm.redis.connection_timeout";


	/**
	 * boolean flag, whether to use SSL. Defaults to false.
	 */
	public static final String SSL = "hibernate.ogm.redis.ssl";

	/**
	 * Property for configuring the strategy for storing entities. Valid values are the
	 * {@link org.hibernate.ogm.datastore.redis.options.EntityStorageType} enumeration and the String
	 * representation of its constants. Defaults to the json storage strategy.
	 * <p/>
	 * Note that any value specified via this property will be overridden by values configured via annotations or the
	 * programmatic API.
	 */
	public static final String ENTITY_STORE = "hibernate.ogm.datastore.redis.entity_storage";

	/**
	 * Configuration property for setting the expiry of keys. This value is set in milliseconds.
	 * <p/>
	 * Defaults to none.
	 *
	 * @see org.hibernate.ogm.datastore.redis.options.TTL
	 */
	public static final String EXPIRY = "hibernate.ogm.redis.expiry";

	private RedisProperties() {
	}
}
