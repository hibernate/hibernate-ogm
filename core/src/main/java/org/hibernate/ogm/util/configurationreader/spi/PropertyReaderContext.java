/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.configurationreader.spi;


/**
 * A context for retrieving the value of a given property.
 *
 * @author Gunnar Morling
 * @param <T> the expected type of the property
 */
public interface PropertyReaderContext<T> {

	/**
	 * Sets a default value in case no value is specified for the given property.
	 */
	PropertyReaderContext<T> withDefault(T defaultValue);

	/**
	 * Marks the given property as required. In this case an exception will be raised if no value is specified for that
	 * property.
	 */
	PropertyReaderContext<T> required();

	/**
	 * Adds a validator used to validate the value of the given property. Several validators can be added.
	 */
	PropertyReaderContext<T> withValidator(PropertyValidator<T> validator);

	/**
	 * Returns a context which allows to specify how the implementation type represented by the given property should be
	 * instantiated.
	 */
	ClassPropertyReaderContext<T> instantiate();

	/**
	 * Returns the value of the specified property.
	 *
	 * @return the value of the specified property; May be {@code null} in case the property is not present in the given
	 * configuration map and no default implementation has been specified
	 * @throws org.hibernate.HibernateException If the property is marked as required but is not present or if one of the registered
	 * {@link PropertyValidator}s detects an invalid value
	 */
	T getValue();
}
