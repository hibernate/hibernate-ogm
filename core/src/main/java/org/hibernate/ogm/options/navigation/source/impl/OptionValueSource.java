/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.source.impl;

import org.hibernate.ogm.options.container.impl.OptionsContainer;

/**
 * A source for option values. Implementations retrieve option values e.g. using annotations, API invocations or
 * configuration values.
 *
 * @author Gunnar Morling
 */
public interface OptionValueSource {

	/**
	 * Returns an {@link OptionsContainer} with global-level options.
	 *
	 * @return an option container with the global-level options; may be empty but never {@code null}
	 */
	OptionsContainer getGlobalOptions();

	/**
	 * Returns an {@link OptionsContainer} with the entity-level options of the given type.
	 *
	 * @param entityType the type to retrieve the options from
	 * @return an option container with the options of the given type; may be empty but never {@code null}
	 */
	OptionsContainer getEntityOptions(Class<?> entityType);

	/**
	 * Returns an {@link OptionsContainer} with the property-level options of the given property.
	 *
	 * @param entityType type declaring the property to retrieve the options from
	 * @param propertyName name of the property to retrieve the options from
	 * @return an option container with options of the given property; may be empty but never {@code null}
	 */
	OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName);
}
