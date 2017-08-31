/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import org.hibernate.ogm.datastore.mongodb.options.BinaryStorage;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.OptionValuePair;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class BinaryStorageConverter implements AnnotationConverter<BinaryStorage> {

	@Override
	public OptionValuePair<?> convert(BinaryStorage annotation) {
		return OptionValuePair.getInstance( new BinaryStorageOption(), annotation.value() );
	}
}
