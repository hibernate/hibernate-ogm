/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation;

import org.hibernate.ogm.options.navigation.spi.BaseEntityContext;
import org.hibernate.ogm.options.navigation.spi.ConfigurationContext;

/**
 * Entry point to the options navigation API. Let's you define global level options as well as navigate to a specific
 * entity level.
 * <p>
 * Implementations must declare a constructor with a single parameter of type {@link ConfigurationContext} and should
 * preferably be derived from {@link BaseEntityContext}.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @param <G> the type of a provider-specific global context definition, following the self-referential generic type
 * pattern
 * @param <E> the type of provider-specific entity context definition, associated with the specific global context type
 */
public interface GlobalContext<G extends GlobalContext<G, E>, E extends EntityContext<E, ?>> {

	/**
	 * Specify mapping for the entity {@code type}
	 */
	E entity(Class<?> type);

}
