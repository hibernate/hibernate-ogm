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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Container for a group of options. Can hold unique as well as non-unique options. While several options of a given
 * non-unique option type can be stored in this container, at most one option of a given unique option type can be
 * stored.
 * <p/>
 * This class is not thread-safe, callers need to synchronize externally when accessing this container from several
 * threads in parallel.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public class OptionsContainer implements Iterable<Option<?>> {

	/**
	 * An immutable empty options container. A {@link java.lang.UnsupportedOperationException} will be raised when
	 * adding or removing elements to/from this container.
	 */
	public static final OptionsContainer EMPTY = new EmptyOptionsContainer();

	/**
	 * Holds all non-unique options, keyed by option type.
	 */
	private final SetMultiMap<Class<?>, Option<?>> options;

	/**
	 * Holds all unique options, keyed by option type.
	 */
	private final Map<Class<?>, Option<?>> uniqueOptions;

	public OptionsContainer(OptionsContainer source) {
		this.options = new SetMultiMap<Class<?>, Option<?>>( source.options.size() );
		this.uniqueOptions = new HashMap<Class<?>, Option<?>>( source.uniqueOptions.size() );

		addAll( source );
	}

	public OptionsContainer() {
		this.options = new SetMultiMap<Class<?>, Option<?>>();
		this.uniqueOptions = new HashMap<Class<?>, Option<?>>();
	}

	@Override
	public Iterator<Option<?>> iterator() {
		Set<Option<?>> allOptions = new HashSet<Option<?>>();

		allOptions.addAll( options.values() );
		allOptions.addAll( uniqueOptions.values() );

		return allOptions.iterator();
	}

	/**
	 * Adds an {@link Option} to this container. Non unique options will be added to the options stored for the given
	 * type, while unique options will replace the existing option of the same type if such exists.
	 *
	 * @param option to add to the container.
	 */
	public void add(Option<?> option) {
		if ( option instanceof UniqueOption ) {
			uniqueOptions.put( option.getClass(), option );
		}
		else {
			options.put( option.getClass(), option );
		}
	}

	/**
	 * Adds all options from the passed container to this container.
	 *
	 * @param container a container with options to add
	 */
	public void addAll(OptionsContainer container) {
		for ( Option<?> option : container.options.values() ) {
			options.put( option.getClass(), option );
		}

		uniqueOptions.putAll( container.uniqueOptions );
	}

	/**
	 * Returns a set with all options of the given type (be it unique or non-unique). The returned set may not be
	 * altered; an exception is thrown when doing so. Note that for obtaining unique options preferably
	 * {@link #getUnique(Class)} should be used.
	 *
	 * @param optionType the type of options to return
	 * @return a set with all options of the given type; may be empty but never {@code null}
	 */
	public <T extends Option<?>> Set<T> get(Class<T> optionType) {
		if ( UniqueOption.class.isAssignableFrom( optionType ) ) {
			@SuppressWarnings("unchecked")
			Set<T> uniqueOption = (Set<T>) getUniqueAsSet( (Class<? extends UniqueOption>) optionType );
			return uniqueOption;
		}

		// safe as the right type is ensured during put
		@SuppressWarnings("unchecked")
		Set<T> optionsOfType = (Set<T>) options.get( optionType );
		return Collections.unmodifiableSet( optionsOfType );
	}

	private <T extends UniqueOption> Set<T> getUniqueAsSet(Class<T> optionType) {
		T option = getUnique( optionType );

		if ( option != null ) {
			return Collections.singleton( option );
		}
		else {
			return Collections.emptySet();
		}
	}

	/**
	 * Returns the unique option of the given type if present in this container.
	 *
	 * @param optionType the type of option to return
	 * @return the unique option with the given type or {@code null} if this option is not present in this container
	 */
	public <T extends UniqueOption> T getUnique(Class<T> optionType) {
		// safe as the right type is ensured during put
		@SuppressWarnings("unchecked")
		T option = (T) uniqueOptions.get( optionType );
		return option;
	}

	@Override
	public String toString() {
		return "OptionsContainer [options=" + options + ", uniqueOptions=" + uniqueOptions.values() + "]";
	}

	private static class EmptyOptionsContainer extends OptionsContainer {

		@Override
		public void add(Option<?> option) {
			throw new UnsupportedOperationException( "No options may be added to this container " );
		}

		@Override
		public void addAll(OptionsContainer container) {
			throw new UnsupportedOperationException( "No options may be added to this container " );
		}
	}

	/**
	 * Map-like structure that associated multiple values with a given key, applying set semantics.
	 *
	 * @author Gunnar Morling
	 */
	private static class SetMultiMap<K, V> {

		private final Map<K, Set<V>> entries;

		public SetMultiMap(int initialCapacity) {
			entries = new HashMap<K, Set<V>>( initialCapacity );
		}

		public SetMultiMap() {
			entries = new HashMap<K, Set<V>>();
		}

		public void put(K key, V value) {
			Set<V> valuesForKey = entries.get( key );

			if ( valuesForKey == null ) {
				valuesForKey = new HashSet<V>( 5 );
				entries.put( key, valuesForKey );
			}

			valuesForKey.add( value );
		}

		public Set<V> values() {
			Set<V> allValues = new HashSet<V>();

			for ( Set<V> valuesOfKey : entries.values() ) {
				allValues.addAll( valuesOfKey );
			}

			return allValues;
		}

		public Set<V> get(K key) {
			return entries.get( key );
		}

		public int size() {
			return entries.size();
		}

		@Override
		public String toString() {
			return entries.toString();
		}
	}
}
