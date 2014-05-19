/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options.navigation.impl;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.impl.AssociationStorageOption;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreEntityContext;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStorePropertyContext;
import org.hibernate.ogm.options.navigation.impl.BaseEntityContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;

/**
 * Converts document store entity-level options.
 *
 * @author Gunnar Morling
 */
public abstract class DocumentStoreEntityContextImpl<E extends DocumentStoreEntityContext<E, P>, P extends DocumentStorePropertyContext<E, P>> extends
		BaseEntityContext<E, P> implements DocumentStoreEntityContext<E, P> {

	public DocumentStoreEntityContextImpl(ConfigurationContext context) {
		super( context );
	}

	@Override
	public E associationStorage(AssociationStorageType associationStorage) {
		addEntityOption( new AssociationStorageOption(), associationStorage );

		// ok; an error would only occur for inconsistently defined context types
		@SuppressWarnings("unchecked")
		E context = (E) this;
		return context;
	}
}
