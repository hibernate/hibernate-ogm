/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.spi;

import java.util.Map;

/**
 * Provides access to the options effectively applying for a given element (e.g. a property or entity type). The
 * following precedence rules apply in case an option is specified more than once at different levels:
 * <ul>
 * <li>Property-level options take precedence over entity-level options which take precedence over global options</li>
 * <li>On each level options specified via the API take precedence over options specified using annotations</li>
 * </ul>
 * The algorithm for resolving option values is described more formally in the following. The algorithm aborts as soon
 * as an option value has been found in any of its steps.
 * <p>
 * <b>Option value resolution algorithm</b>
 * <ol>
 * <li>If the given element represents a property,
 * <ol>
 * <li>look for an option value configured on the property itself,</li>
 * <li>iteratively repeat step 1.1. for super-class properties overridden by the given property, walking up the entire
 * inheritance hierarchy beginning with the direct super-class of the entity-type hosting the given property.</li>
 * </ol>
 * </li>
 * <li>If the given element represents an entity type,
 * <ol>
 * <li>look for an option value configured on the entity itself,</li>
 * <li>iteratively repeat step 2.1 for the super-classes of the given entity type, walking up the entire inheritance
 * hierarchy beginning with the entity type's direct super-class.</li>
 * </ol>
 * </li>
 * <li>Look for an option value configured via the programmatic API on the global level</li>
 * <li>As measure of last resort, look for an option value specified via a configuration property in
 * {@code persistence.xml} etc.</li>
 * </ol>
 *
 * @author Gunnar Morling
 */
public interface OptionsContext {

	/**
	 * Returns the value of the option with the given identifier, if present. Note that for obtaining unique options
	 * preferably {@link #getUnique(Class)} should be used.
	 *
	 * @param optionType the type of option to return the value of
	 * @param identifier the identifier of the option to return the value of
	 * @return the value of the specified option or {@code null} if no value is present
	 */
	<I, V, O extends Option<I, V>> V get(Class<O> optionType, I identifier);

	/**
	 * Returns the value of the unique option of the given type, if present.
	 *
	 * @param optionType the type of option to return
	 * @return the unique option with the given type or {@code null} if this option is not present
	 */
	<V, O extends UniqueOption<V>> V getUnique(Class<O> optionType);

	/**
	 * Returns all values of the specified option type, keyed by identifier. Note that unique options should preferably
	 * be obtained via {@link #getUnique(Class)}.
	 *
	 * @param optionType the type of option to return
	 * @return a map with all values of the specified option, keyed by identifier. May be empty but never {@code null}
	 */
	<I, V, O extends Option<I, V>> Map<I, V> getAll(Class<O> optionType);
}
