/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.document.options.navigation.spi;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreEntityContext;
import org.hibernate.ogm.datastore.document.options.navigation.DocumentStoreGlobalContext;
import org.hibernate.ogm.datastore.document.options.spi.AssociationStorageOption;
import org.hibernate.ogm.options.navigation.spi.BaseGlobalContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Converts global document store options.
 *
 * @author Gunnar Morling
 */
public abstract class BaseDocumentStoreGlobalContext<G extends DocumentStoreGlobalContext<G, E>, E extends DocumentStoreEntityContext<E, ?>> extends
		BaseGlobalContext<G, E> implements DocumentStoreGlobalContext<G, E> {

	public BaseDocumentStoreGlobalContext(ConfigurationContext context) {
		super( context );
	}

	@Override
	public G associationStorage(AssociationStorageType associationStorage) {
		addGlobalOption( new AssociationStorageOption(), associationStorage );

		// ok; an error would only occur for inconsistently defined context types
		@SuppressWarnings("unchecked")
		G context = (G) this;
		return context;
	}
}
