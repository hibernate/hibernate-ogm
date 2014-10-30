/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.dialect.impl;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.spi.AssociationKind;

/**
 * Represents a strategy for storing associations in MongoDB. Provides an aggregated view on {@link AssociationKind} as
 * well as the {@link AssociationStorageType} and {@link AssociationDocumentType} options.
 *
 * @author Gunnar Morling
 */
public enum AssociationStorageStrategy {

	IN_ENTITY, GLOBAL_COLLECTION, COLLECTION_PER_ASSOCIATION;

	public static AssociationStorageStrategy getInstance(AssociationKeyMetadata keyMetadata, AssociationStorageType associationStorage, AssociationDocumentType associationDocumentStorage) {
		if ( keyMetadata.isOneToOne() || keyMetadata.getAssociationKind() == AssociationKind.EMBEDDED_COLLECTION || associationStorage == AssociationStorageType.IN_ENTITY ) {
			return IN_ENTITY;
		}
		else if ( associationDocumentStorage == AssociationDocumentType.COLLECTION_PER_ASSOCIATION ) {
			return COLLECTION_PER_ASSOCIATION;
		}
		else {
			return GLOBAL_COLLECTION;
		}
	}
}
