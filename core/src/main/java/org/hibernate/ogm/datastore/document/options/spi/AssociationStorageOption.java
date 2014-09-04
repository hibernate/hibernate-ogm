/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options.spi;

import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Represents the type of association storage as configured via the API or annotations for a given element.
 *
 * @author Gunnar Morling
 */
public class AssociationStorageOption extends UniqueOption<AssociationStorageType> {

	private static final AssociationStorageType DEFAULT_ASSOCIATION_STORAGE = AssociationStorageType.IN_ENTITY;

	@Override
	public AssociationStorageType getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( DocumentStoreProperties.ASSOCIATIONS_STORE, AssociationStorageType.class )
				.withDefault( DEFAULT_ASSOCIATION_STORAGE )
				.getValue();
	}
}
