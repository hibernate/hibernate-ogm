/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.options.cache.impl;

import org.hibernate.ogm.datastore.infinispanremote.options.cache.CacheTemplate;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

public class CacheTemplateOption extends UniqueOption<CacheTemplate> {

	@Override
	public CacheTemplate getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return null;
	}
}
