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
import java.util.Map;
import java.util.Map.Entry;

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
public class OptionsContainer {

	/**
	 * An immutable empty options container. A {@link java.lang.UnsupportedOperationException} will be raised when
	 * adding or removing elements to/from this container.
	 */
	public static final OptionsContainer EMPTY = new EmptyOptionsContainer();

	@SuppressWarnings("rawtypes")
	private static final ValueContainer<?, ?> EMPTY_VALUE_CONTAINER = new EmptyValueContainer();

	/**
	 * Holds all the options of this container, keyed by option type. The value container for an option will hold one or
	 * more than one value depending on whether the option is unique or not.
	 */
	private final Map<Class<? extends Option<?, ?>>, ValueContainer<?, ?>> optionValues;

	public OptionsContainer(OptionsContainer source) {
		this.optionValues = new HashMap<Class<? extends Option<?, ?>>, ValueContainer<?, ?>>( source.optionValues.size() );
		addAll( source );
	}

	public OptionsContainer() {
		this.optionValues = new HashMap<Class<? extends Option<?, ?>>, ValueContainer<?, ?>>();
	}

	/**
	 * Adds an {@link Option} with the given value to this container.
	 *
	 * @param option to add to the container
	 * @param value the value of the option to add
	 */
	public <I, V> void add(Option<I, V> option, V value) {
		getOrCreateValueContainer( option ).add( option.getOptionIdentifier(), value );
	}

	/**
	 * Adds all options from the passed container to this container.
	 *
	 * @param container a container with options to add
	 */
	public void addAll(OptionsContainer container) {
		for ( Entry<Class<? extends Option<?, ?>>, ValueContainer<?, ?>> entry : container.optionValues.entrySet() ) {
			addAll( entry.getKey(), entry.getValue() );
		}
	}

	/**
	 * Returns the value of the given option with the given identifier, if present in this container. Note that for
	 * obtaining unique options preferably {@link #getUnique(Class)} should be used.
	 *
	 * @param optionType the type of option to return the value of
	 * @param identifier the identifier of the option to return the value of
	 * @return the value of the specified option or {@code null} if no value is present
	 */
	public <I, V> V get(Class<? extends Option<I, V>> optionType, I identifier) {
		return getNonNullValueContainer( optionType ).get( identifier );
	}

	/**
	 * Returns the value of the unique option of the given type, if present in this container.
	 *
	 * @param optionType the type of option to return
	 * @return the unique option with the given type or {@code null} if this option is not present in this container
	 */
	public <V> V getUnique(Class<? extends UniqueOption<V>> optionType) {
		return getNonNullValueContainer( optionType ).getUnique();
	}

	/**
	 * Returns all values of the specified option type, keyed by identifier. Note that unique options should preferably
	 * be obtained via {@link #getUnique(Class)}.
	 *
	 * @param optionType the type of option to return
	 * @return a map with all values of the specified option, keyed by identifier. May be empty but never {@code null}
	 */
	public <I, V, T extends Option<I, V>> Map<I, V> getAll(Class<T> optionType) {
		return getNonNullValueContainer( optionType ).getAll();
	}

	@Override
	public String toString() {
		return "OptionsContainer [optionValues=" + optionValues + "]";
	}

	@SuppressWarnings("unchecked")
	private <I, V> void addAll(Class<? extends Option<?, ?>> optionType, ValueContainer<?, ?> values) {
		ValueContainer<I, V> valueContainer = getOrCreateValueContainer( (Class<? extends Option<I, V>>) optionType );
		valueContainer.addAll( ( (ValueContainer<I, V>) values ).getAll() );
	}

	private <V, I> ValueContainer<I, V> getOrCreateValueContainer(Option<I, V> option) {
		@SuppressWarnings("unchecked")
		Class<? extends Option<I, V>> optionType = (Class<? extends Option<I, V>>) option.getClass();
		return getOrCreateValueContainer( optionType );
	}

	private <V, I> ValueContainer<I, V> getOrCreateValueContainer(Class<? extends Option<I, V>> optionType) {
		ValueContainer<I, V> values = getValueContainer( optionType );

		if ( values == null ) {
			values = createValueContainer( optionType );
			optionValues.put( optionType, values );
		}

		return values;
	}

	private <V, I> ValueContainer<I, V> getNonNullValueContainer(Class<? extends Option<I, V>> optionType) {
		ValueContainer<I, V> values = getValueContainer( optionType );
		return values != null ? values : this.<I, V>getEmptyValueContainer();
	}

	private <V, I> ValueContainer<I, V> getValueContainer(Class<? extends Option<I, V>> optionType) {
		@SuppressWarnings("unchecked")
		ValueContainer<I, V> values = (ValueContainer<I, V>) optionValues.get( optionType );
		return values;
	}

	private <I, V> ValueContainer<I, V> createValueContainer(Class<? extends Option<I, V>> optionType) {
		if ( UniqueOption.class.isAssignableFrom( optionType ) ) {
			return new UniqueValueContainer<I, V>();
		}
		else {
			return new NonUniqueValueContainer<I, V>();
		}
	}

	@SuppressWarnings("unchecked")
	private <I, V> ValueContainer<I, V> getEmptyValueContainer() {
		return (ValueContainer<I, V>) EMPTY_VALUE_CONTAINER;
	}

	/**
	 * Implementations store one or more values of a given option, depending on whether the option is unique or not.
	 */
	private interface ValueContainer<I, V> {

		void add(I identifier, V value);

		void addAll(Map<I, V> values);

		V get(I identifier);

		V getUnique();

		Map<I, V> getAll();
	}

	private static class UniqueValueContainer<I, V> implements ValueContainer<I, V> {

		private I identifier;
		private V value;

		@Override
		public void add(I identifier, V value) {
			this.identifier = identifier;
			this.value = value;
		}

		@Override
		public void addAll(Map<I, V> values) {
			for ( Entry<I, V> entry : values.entrySet() ) {
				identifier = entry.getKey();
				value = entry.getValue();
			}
		}

		@Override
		public V get(I identifier) {
			return value;
		}

		@Override
		public V getUnique() {
			return value;
		}

		@Override
		public Map<I, V> getAll() {
			return Collections.singletonMap( identifier, value );
		}

		@Override
		public String toString() {
			return "UniqueValueContainer [identifier=" + identifier + ", value=" + value + "]";
		}
	}

	private static class NonUniqueValueContainer<I, V> implements ValueContainer<I, V> {

		private final Map<I, V> values = new HashMap<I, V>();

		@Override
		public void add(I identifier, V value) {
			values.put( identifier, value );
		}

		@Override
		public void addAll(Map<I, V> values) {
			this.values.putAll( values );
		}

		@Override
		public V get(I identifier) {
			return values.get( identifier );
		}

		@Override
		public V getUnique() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<I, V> getAll() {
			return values;
		}

		@Override
		public String toString() {
			return "NonUniqueValueContainer [values=" + values + "]";
		}
	}

	private static class EmptyValueContainer<I, V> implements ValueContainer<I, V> {

		@Override
		public void add(I identifier, V value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addAll(Map<I, V> values) {
			throw new UnsupportedOperationException();
		}

		@Override
		public V get(I identifier) {
			return null;
		}

		@Override
		public V getUnique() {
			return null;
		}

		@Override
		public Map<I, V> getAll() {
			return Collections.emptyMap();
		}
	}

	private static class EmptyOptionsContainer extends OptionsContainer {

		@Override
		public <I, V> void add(Option<I, V> option, V value) {
			throw new UnsupportedOperationException( "No options may be added to this container " );
		}

		@Override
		public void addAll(OptionsContainer container) {
			throw new UnsupportedOperationException( "No options may be added to this container " );
		}
	}
}
