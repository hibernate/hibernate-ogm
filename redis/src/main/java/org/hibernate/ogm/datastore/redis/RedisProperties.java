/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis;

import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.keyvalue.cfg.KeyValueStoreProperties;

/**
 * Properties for configuring the Redis datastore via {@code persistence.xml} or {@link OgmConfiguration}.
 *
 * @author Mark Paluch
 */
public final class RedisProperties implements KeyValueStoreProperties, DocumentStoreProperties {

	/**
	 * The timeout used at the connection to the Redis instance. This value is set in milliseconds. Defaults to 5000.
	 */
	public static final String TIMEOUT = "hibernate.ogm.redis.connection_timeout";

	/**
	 * boolean flag, whether to use SSL. Defaults to false.
	 */
	public static final String SSL = "hibernate.ogm.redis.ssl";

	/**
	 * boolean flag, whether to use Redis Cluster. Defaults to false.
	 */
	public static final String CLUSTER = "hibernate.ogm.redis.cluster";

	/**
	 * Configuration property for setting the expiry of keys. This value is set in milliseconds.
	 * <p>
	 * Defaults to none.
	 *
	 * @see org.hibernate.ogm.datastore.redis.options.TTL
	 */
	public static final String TTL = "hibernate.ogm.redis.ttl";

	private RedisProperties() {
	}
}
