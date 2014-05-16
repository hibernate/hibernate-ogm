/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.container.impl;

import java.util.Map;

import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.UniqueOption;

/**
 * Provides the option values configured on a specific element such as an entity or property, through one
 * {@link OptionValueSource}. Instances should preferably be immutable and be obtained via
 * {@link OptionsContainerBuilder}.
 *
 * @author Gunnar Morling
 */
public interface OptionsContainer {

	OptionsContainer EMPTY = new EmptyOptionsContainer();

	/**
	 * Returns the value of the given option with the given identifier, if present in this container. Note that for
	 * obtaining unique options preferably {@link #getUnique(Class)} should be used.
	 *
	 * @param optionType the type of option to return the value of
	 * @param identifier the identifier of the option to return the value of
	 * @return the value of the specified option or {@code null} if no value is present
	 */
	<I, V> V get(Class<? extends Option<I, V>> optionType, I identifier);

	/**
	 * Returns the value of the unique option of the given type, if present in this container.
	 *
	 * @param optionType the type of option to return
	 * @return the unique option with the given type or {@code null} if this option is not present in this container
	 */
	<V> V getUnique(Class<? extends UniqueOption<V>> optionType);

	/**
	 * Returns all values of the specified option type, keyed by identifier. Note that unique options should preferably
	 * be obtained via {@link #getUnique(Class)}.
	 *
	 * @param optionType the type of option to return
	 * @return a map with all values of the specified option, keyed by identifier. May be empty but never {@code null}
	 */
	<I, V, T extends Option<I, V>> Map<I, V> getAll(Class<T> optionType);
}
