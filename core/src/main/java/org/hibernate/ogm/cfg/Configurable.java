/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg;

import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.GlobalContext;

/**
 * Implementations allow to apply configuration options specific to given datastores.
 *
 * @author Gunnar Morling
 */
public interface Configurable {

	/**
	 * Applies configuration options to the bootstrapped session factory.
	 *
	 * @param datastoreType represents the datastore to be configured
	 * @return a context object representing the entry point into the fluent configuration API
	 */
	<D extends DatastoreConfiguration<G>, G extends GlobalContext<?, ?>> G configureOptionsFor(Class<D> datastoreType);
}
