/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.options.impl;

import org.hibernate.ogm.datastore.redis.options.TTL;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.OptionValuePair;

/**
 * Converts {@link TTL} instances into an equivalent option value pair. The TTL is set in milliseconds.
 *
 * @author Mark Paluch
 */
public class TTLConverter implements AnnotationConverter<TTL> {

	@Override
	public OptionValuePair<?> convert(TTL annotation) {
		return OptionValuePair.getInstance( new TTLOption(), annotation.unit().toMillis( annotation.value() ) );
	}
}
