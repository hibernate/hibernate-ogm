/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options.impl;

import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.options.spi.UniqueOption;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Specifies whether association documents should be stored in a separate collection per association type or in one
 * global collection for all associations.
 *
 * @author Gunnar Morling
 */
public class AssociationDocumentStorageOption extends UniqueOption<AssociationDocumentType> {

	@Override
	public AssociationDocumentType getDefaultValue(ConfigurationPropertyReader propertyReader) {
		return propertyReader.property( MongoDBProperties.ASSOCIATION_DOCUMENT_STORAGE, AssociationDocumentType.class )
				.withDefault( AssociationDocumentType.GLOBAL_COLLECTION )
				.getValue();
	}
}
