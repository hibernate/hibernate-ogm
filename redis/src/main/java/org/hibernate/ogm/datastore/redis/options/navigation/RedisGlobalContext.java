/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.options.navigation;

import java.util.concurrent.TimeUnit;

import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreGlobalContext;
import org.hibernate.ogm.datastore.keyvalue.options.navigation.KeyValueStoreGlobalContext;

/**
 * Allows to configure Redis-specific options applying on a global level. These options may be overridden for single
 * entities or properties.
 *
 * @author Mark Paluch
 */
public interface RedisGlobalContext extends KeyValueStoreGlobalContext<RedisGlobalContext, RedisEntityContext>,
		DocumentStoreGlobalContext<RedisGlobalContext, RedisEntityContext> {

	/**
	 * Specifies the TTL for keys. See also {@link org.hibernate.ogm.datastore.redis.options.TTL}
	 *
	 * @param value the TTL duration
	 * @param timeUnit the TTL time unit
	 *
	 * @return this context, allowing for further fluent API invocations
	 */
	RedisGlobalContext ttl(long value, TimeUnit timeUnit);
}
