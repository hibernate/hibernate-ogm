/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.options.impl;

import org.hibernate.ogm.datastore.ignite.options.CollocatedAssociation;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.OptionValuePair;

/**
 * Converts {@link CollocatedAssociation} instances into an equivalent option value pair.
 *
 * @author Victor Kadachigov
 */
public class CollocatedAssociationConverter implements AnnotationConverter<CollocatedAssociation> {

	@Override
	public OptionValuePair<?> convert(CollocatedAssociation annotation) {
		Boolean value = annotation.value();
		return OptionValuePair.getInstance( new CollocatedAssociationOption(), value );
	}

}
