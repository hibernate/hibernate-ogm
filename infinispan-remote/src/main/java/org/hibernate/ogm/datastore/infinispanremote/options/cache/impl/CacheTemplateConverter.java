/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.options.cache.impl;

import org.hibernate.ogm.datastore.infinispanremote.options.cache.CacheTemplate;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.OptionValuePair;

public class CacheTemplateConverter implements AnnotationConverter<CacheTemplate> {
	@Override
	public OptionValuePair<?> convert(CacheTemplate annotation) {
		return OptionValuePair.getInstance( new CacheTemplateOption(), annotation );
	}
}
