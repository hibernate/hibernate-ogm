/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.options.impl;

import org.hibernate.ogm.datastore.redis.RedisProperties;
import org.hibernate.ogm.datastore.redis.options.EntityStorageType;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Represents the type of entity storage as configured via the API or annotations for a given element.
 *
 * @author Mark Paluch
 */
public class EntityStorageOption extends UniqueOption<EntityStorageType> {

	private static final EntityStorageType DEFAULT_ENTITY_STORAGE = EntityStorageType.JSON;

	@Override
	public EntityStorageType getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( RedisProperties.ENTITY_STORE, EntityStorageType.class )
				.withDefault( DEFAULT_ENTITY_STORAGE )
				.getValue();
	}
}
