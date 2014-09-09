/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options.navigation.spi;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreEntityContext;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStorePropertyContext;
import org.hibernate.ogm.datastore.document.options.spi.AssociationStorageOption;
import org.hibernate.ogm.options.navigation.spi.BasePropertyContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Converts document store property-level options.
 *
 * @author Gunnar Morling
 */
public abstract class BaseDocumentStorePropertyContext<E extends DocumentStoreEntityContext<E, P>, P extends DocumentStorePropertyContext<E, P>> extends
		BasePropertyContext<E, P> implements DocumentStorePropertyContext<E, P> {

	public BaseDocumentStorePropertyContext(ConfigurationContext context) {
		super( context );
	}

	@Override
	public P associationStorage(AssociationStorageType storage) {
		addPropertyOption( new AssociationStorageOption(), storage );

		// ok; an error would only occur for inconsistently defined context types
		@SuppressWarnings("unchecked")
		P context = (P) this;
		return context;
	}
}
