/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options.impl;

import org.hibernate.ogm.datastore.document.options.AssociationStorage;
import org.hibernate.ogm.datastore.document.options.spi.AssociationStorageOption;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.OptionValuePair;

/**
 * Converts {@link AssociationStorage} instances into an equivalent option value pair.
 *
 * @author Gunnar Morling
 */
public class AssociationStorageConverter implements AnnotationConverter<AssociationStorage> {

	@Override
	public OptionValuePair<?> convert(AssociationStorage annotation) {
		return OptionValuePair.getInstance( new AssociationStorageOption(), annotation.value() );
	}
}
