/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.mongodb.impl;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.options.AssociationDocumentType;
import org.hibernate.ogm.grid.AssociationKind;

/**
 * Represents a strategy for storing associations in MongoDB. Provides an aggregated view on {@link AssociationKind} as
 * well as the {@link AssociationStorageType} and {@link AssociationDocumentType} options.
 *
 * @author Gunnar Morling
 */
public class AssociationStorageStrategy {

	private final boolean isEmbeddedInEntity;
	private final boolean isGlobalCollection;

	private AssociationStorageStrategy(boolean isEmbeddedInEntity, boolean isGlobalCollection) {
		this.isEmbeddedInEntity = isEmbeddedInEntity;
		this.isGlobalCollection = isGlobalCollection;
	}

	public static AssociationStorageStrategy getInstance(AssociationKind associationKind, AssociationStorageType associationStorage, AssociationDocumentType associationDocumentStorage) {
		if ( associationKind == AssociationKind.EMBEDDED_COLLECTION || associationStorage == AssociationStorageType.IN_ENTITY ) {
			return new AssociationStorageStrategy( true, false );
		}
		else if ( associationDocumentStorage == AssociationDocumentType.COLLECTION_PER_ASSOCIATION ) {
			return new AssociationStorageStrategy( false, false );
		}
		else {
			return new AssociationStorageStrategy( false, true );
		}
	}

	/**
	 * Whether the given association is to be stored embedded within the document of the owning entity. That's the case
	 * if the association is either an embedded element collection or the association is configured as to be stored
	 * embedded as per the {@link AssociationStorageType} and {@link AssociationDocumentType} options.
	 *
	 * @return {@code true} if this association is to be stored embedded within the document of the owning entity,
	 * {@code false} otherwise.
	 */
	public boolean isEmbeddedInEntity() {
		return isEmbeddedInEntity;
	}

	public boolean isGlobalCollection() {
		return isGlobalCollection;
	}

	public boolean isCollectionPerAssociation() {
		return !isEmbeddedInEntity && !isGlobalCollection;
	}
}
