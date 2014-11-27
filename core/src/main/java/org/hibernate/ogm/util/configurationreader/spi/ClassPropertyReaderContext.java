/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.configurationreader.spi;

/**
 * A {@link PropertyReaderContext} which allows to retrieve properties by instantiating a given implementation type,
 * e.g. specified as fully-qualified class name or class object.
 *
 * @author Gunnar Morling
 * @param <T> the type of the property
 */
public interface ClassPropertyReaderContext<T> extends PropertyReaderContext<T> {

	/**
	 * Sets the default implementation type for the property in case no value is found.
	 *
	 * @param defaultImplementation the implementation to use if none is configured
	 * @return the class property reader context
	 */
	ClassPropertyReaderContext<T> withDefaultImplementation(Class<? extends T> defaultImplementation);

	/**
	 * Sets the name of default implementation type for the property in case no value is found.
	 *
	 * @param defaultImplementationName the implementation to use if none is configured
	 * @return the class property reader context
	 */
	ClassPropertyReaderContext<T> withDefaultImplementation(String defaultImplementationName);

	/**
	 * Sets a short name resolver to be applied in case the property is given as string
	 *
	 * @param shortNameResolver the resolver to use when an implementation short name is provided
	 * @return the class property reader context
	 */
	ClassPropertyReaderContext<T> withShortNameResolver(ShortNameResolver shortNameResolver);

	/**
	 * Get type of the of the property
	 *
	 * @return the type of the property
	 */
	T getTypedValue();
}
