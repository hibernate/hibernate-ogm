/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.options.navigation.impl;

import org.hibernate.ogm.datastore.keyvalue.options.navigation.spi.BaseKeyValueStorePropertyContext;
import org.hibernate.ogm.datastore.redis.navigation.RedisEntityContext;
import org.hibernate.ogm.datastore.redis.options.navigation.RedisPropertyContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Converts Redis property-level options.
 *
 * @author Gunnar Morling
 */
public abstract class RedisPropertyContextImpl
		extends BaseKeyValueStorePropertyContext<RedisEntityContext, RedisPropertyContext> implements
		RedisPropertyContext {

	public RedisPropertyContextImpl(ConfigurationContext context) {
		super( context );
	}
}
