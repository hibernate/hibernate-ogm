/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.options.navigation.impl;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.spi.AssociationStorageOption;
import org.hibernate.ogm.datastore.keyvalue.options.navigation.spi.BaseKeyValueStorePropertyContext;
import org.hibernate.ogm.datastore.redis.options.navigation.RedisEntityContext;
import org.hibernate.ogm.datastore.redis.options.navigation.RedisPropertyContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;
import org.hibernate.ogm.util.impl.Contracts;

/**
 * Converts Redis property-level options.
 *
 * @author Mark Paluch
 */
public abstract class RedisPropertyContextImpl
		extends BaseKeyValueStorePropertyContext<RedisEntityContext, RedisPropertyContext> implements
		RedisPropertyContext {

	public RedisPropertyContextImpl(ConfigurationContext context) {
		super( context );
	}

	@Override
	public RedisPropertyContext associationStorage(AssociationStorageType associationStorageType) {
		Contracts.assertParameterNotNull( associationStorageType, "associationStorageType" );
		addEntityOption( new AssociationStorageOption(), associationStorageType );
		return this;
	}
}
