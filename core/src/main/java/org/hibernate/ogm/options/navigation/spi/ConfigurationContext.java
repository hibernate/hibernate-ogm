/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.spi;

import org.hibernate.ogm.options.navigation.EntityContext;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.PropertyContext;
import org.hibernate.ogm.options.spi.Option;

/**
 * Keeps track of the entities and properties configured using the fluent configuration API.
 * <p>
 * There is one instance of this context per invocation of this API (beginning with the creation of a
 * {@link GlobalContext}). This instance is passed between the individual context types created in the course of using
 * the fluent API.
 *
 * @author Gunnar Morling
 */
public interface ConfigurationContext {

	<V> void addGlobalOption(Option<?, V> option, V value);

	<V> void addEntityOption(Option<?, V> option, V value);

	<V> void addPropertyOption(Option<?, V> option, V value);

	/**
	 * Creates a new {@link GlobalContext} object based on the given context implementation types. All implementation
	 * types must declare a public or protected constructor with a single parameter, accepting {@link ConfigurationContext}.
	 * <p>
	 * Each context implementation type must provide an implementation of the method(s) declared on the particular
	 * provider-specific context interface. All methods declared on context super interfaces - {@code entity()} and
	 * {@code property()} - are implemented following the dynamic proxy pattern, the implementation types therefore can
	 * be declared abstract, avoiding the need to implement these methods themselves.
	 * <p>
	 * By convention, the implementation types should directly or indirectly extend {@link BaseContext}.
	 *
	 * @param globalContextImplType the provider-specific global context implementation type
	 * @param entityContextImplType the provider-specific entity context implementation type
	 * @param propertyContextImplType the provider-specific property context implementation type
	 * @return a new {@link GlobalContext} object based on the given context implementation types
	 */
	<G extends GlobalContext<?, ?>> G createGlobalContext(
			Class<? extends G> globalContextImplType,
			Class<? extends EntityContext<?, ?>> entityContextImplType,
			Class<? extends PropertyContext<?, ?>> propertyContextImplType
	);
}
