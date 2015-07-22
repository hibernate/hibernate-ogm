/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.options.impl;

import org.hibernate.ogm.datastore.redis.RedisProperties;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Option for specifying the <a href="http://redis.io/commands/pexpireat">expiry</a> in
 * Redis.
 *
 * @author Mark Paluch
 */
public class TTLOption extends UniqueOption<Long> {

	@Override
	public Long getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return propertyReader
				.property( RedisProperties.EXPIRY, Long.class )
				.withDefault( null )
				.getValue();
	}
}
