/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentStorage;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.OptionValuePair;

/**
 * Converts {@link AssociationDocumentStorage} instances into an equivalent option value pair.
 *
 * @author Gunnar Morling
 */
public class AssociationDocumentStorageConverter implements AnnotationConverter<AssociationDocumentStorage> {

	@Override
	public OptionValuePair<?> convert(AssociationDocumentStorage annotation) {
		return OptionValuePair.getInstance( new AssociationDocumentStorageOption(), annotation.value() );
	}
}
