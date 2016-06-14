/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.options.navigation.impl;

import java.util.concurrent.TimeUnit;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.MapStorageType;
import org.hibernate.ogm.datastore.document.options.spi.AssociationStorageOption;
import org.hibernate.ogm.datastore.document.options.spi.MapStorageOption;
import org.hibernate.ogm.datastore.keyvalue.options.navigation.spi.BaseKeyValueStoreGlobalContext;
import org.hibernate.ogm.datastore.redis.options.impl.TTLOption;
import org.hibernate.ogm.datastore.redis.options.navigation.RedisEntityContext;
import org.hibernate.ogm.datastore.redis.options.navigation.RedisGlobalContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;
import org.hibernate.ogm.util.impl.Contracts;

/**
 * Converts global Redis options.
 *
 * @author Mark Paluch
 */
public abstract class RedisGlobalContextImpl
		extends BaseKeyValueStoreGlobalContext<RedisGlobalContext, RedisEntityContext> implements
		RedisGlobalContext {

	public RedisGlobalContextImpl(ConfigurationContext context) {
		super( context );
	}

	@Override
	public RedisGlobalContext associationStorage(AssociationStorageType associationStorageType) {
		Contracts.assertParameterNotNull( associationStorageType, "associationStorageType" );
		addGlobalOption( new AssociationStorageOption(), associationStorageType );
		return this;
	}

	@Override
	public RedisGlobalContext ttl(
			long value, TimeUnit timeUnit) {
		Contracts.assertTrue( value > 0, "value must be greater than 0" );
		Contracts.assertParameterNotNull( timeUnit, "timeUnit" );
		addGlobalOption( new TTLOption(), timeUnit.toMillis( value ) );
		return this;
	}

	@Override
	public RedisGlobalContext mapStorage(MapStorageType mapStorage) {
		Contracts.assertParameterNotNull( mapStorage, "mapStorage" );
		addGlobalOption( new MapStorageOption(), mapStorage );
		return this;
	}
}
