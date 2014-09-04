/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.spi;

import org.hibernate.ogm.cfg.Configurable;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Implementations represent a specific datastore to the user and allow to apply store-specific configuration settings.
 * <p>
 * Implementations must provide a no-args constructor.
 *
 * @author Gunnar Morling
 * @param <G> the type of {@link GlobalContext} supported by the represented datastore
 * @see Configurable#configureOptionsFor(Class)
 */
public interface DatastoreConfiguration<G extends GlobalContext<?, ?>> {

	/**
	 * Returns a new store-specific {@link GlobalContext} instance. Used by the Hibernate OGM engine during
	 * bootstrapping a session factory, not intended for client use.
	 *
	 * @param context configuration context to be used as factory for creating the global context object
	 * @return a new {@link GlobalContext}
	 */
	G getConfigurationBuilder(ConfigurationContext context);
}
