/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
 * </ol>
 * For each step values set via the programmatic API take precedence over values set via annotations in case both are
 * given.
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
	<I, V> V get(Class<? extends Option<I, V>> optionType, I identifier);

	/**
	 * Returns the value of the unique option of the given type, if present.
	 *
	 * @param optionType the type of option to return
	 * @return the unique option with the given type or {@code null} if this option is not present
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
