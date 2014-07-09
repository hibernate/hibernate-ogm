/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.container.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newHashMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.UniqueOption;

/**
 * Container for a group of options. Can hold unique as well as non-unique options. While several options of a given
 * non-unique option type can be stored in this container, at most one option of a given unique option type can be
 * stored.
 * <p>
 * This class is not thread-safe, callers need to synchronize externally when accessing this container from several
 * threads in parallel.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class OptionsContainerBuilder {

	/**
	 * Holds all the options of this container, keyed by option type. The value container for an option will hold one or
	 * more than one value depending on whether the option is unique or not.
	 */
	private final Map<Class<? extends Option<?, ?>>, ValueContainerBuilder<?, ?>> optionValues;

	public OptionsContainerBuilder() {
		this.optionValues = newHashMap();
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
	public void addAll(OptionsContainerBuilder container) {
		for ( Entry<Class<? extends Option<?, ?>>, ValueContainerBuilder<?, ?>> entry : container.optionValues.entrySet() ) {
			addAll( entry.getKey(), entry.getValue().build() );
		}
	}

	public OptionsContainer build() {
		if ( optionValues.isEmpty() ) {
			return OptionsContainer.EMPTY;
		}

		Map<Class<? extends Option<?, ?>>, ValueContainer<?, ?>> values = newHashMap( optionValues.size() );

		for ( Entry<Class<? extends Option<?, ?>>, ValueContainerBuilder<?, ?>> option : optionValues.entrySet() ) {
			values.put( option.getKey(), option.getValue().build() );
		}

		return new ImmutableOptionsContainer( values );
	}

	@Override
	public String toString() {
		return "OptionsContainerBuilder [optionValues=" + optionValues + "]";
	}

	@SuppressWarnings("unchecked")
	private <I, V> void addAll(Class<? extends Option<?, ?>> optionType, ValueContainer<?, ?> values) {
		ValueContainerBuilder<I, V> valueContainer = getOrCreateValueContainer( (Class<? extends Option<I, V>>) optionType );
		valueContainer.addAll( ( (ValueContainer<I, V>) values ).getAll() );
	}

	private <V, I> ValueContainerBuilder<I, V> getOrCreateValueContainer(Option<I, V> option) {
		@SuppressWarnings("unchecked")
		Class<? extends Option<I, V>> optionType = (Class<? extends Option<I, V>>) option.getClass();
		return getOrCreateValueContainer( optionType );
	}

	private <V, I> ValueContainerBuilder<I, V> getOrCreateValueContainer(Class<? extends Option<I, V>> optionType) {
		ValueContainerBuilder<I, V> values = getValueContainer( optionType );

		if ( values == null ) {
			values = createValueContainer( optionType );
			optionValues.put( optionType, values );
		}

		return values;
	}

	private <V, I> ValueContainerBuilder<I, V> getValueContainer(Class<? extends Option<I, V>> optionType) {
		@SuppressWarnings("unchecked")
		ValueContainerBuilder<I, V> values = (ValueContainerBuilder<I, V>) optionValues.get( optionType );
		return values;
	}

	private <I, V> ValueContainerBuilder<I, V> createValueContainer(Class<? extends Option<I, V>> optionType) {
		if ( UniqueOption.class.isAssignableFrom( optionType ) ) {
			return new UniqueValueContainerBuilder<I, V>();
		}
		else {
			return new NonUniqueValueContainerBuilder<I, V>();
		}
	}

	/**
	 * Implementations store one or more values of a given option, depending on whether the option is unique or not.
	 */
	interface ValueContainer<I, V> {

		V get(I identifier);

		V getUnique();

		Map<I, V> getAll();
	}

	private static class UniqueValueContainer<I, V> implements ValueContainer<I, V> {

		private final I identifier;
		private final V value;

		public UniqueValueContainer(I identifier, V value) {
			this.identifier = identifier;
			this.value = value;
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

		private final Map<I, V> values;

		public NonUniqueValueContainer(Map<I, V> values) {
			this.values = Collections.unmodifiableMap( values );
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

	/**
	 * Implementations build {@link ValueContainer} instances.
	 *
	 * @author Gunnar Morling
	 */
	private interface ValueContainerBuilder<I, V> {

		void add(I identifier, V value);

		void addAll(Map<I, V> values);

		ValueContainer<I, V> build();
	}

	private static class UniqueValueContainerBuilder<I, V> implements ValueContainerBuilder<I, V> {

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
		public ValueContainer<I, V> build() {
			return new UniqueValueContainer<I, V>( identifier, value );
		}
	}

	private static class NonUniqueValueContainerBuilder<I, V> implements ValueContainerBuilder<I, V> {

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
		public ValueContainer<I, V> build() {
			return new NonUniqueValueContainer<I, V>( values );
		}
	}

	/**
	 * An immutable {@link OptionsContainer}.
	 *
	 * @author Gunnar Morling
	 */
	private static class ImmutableOptionsContainer implements OptionsContainer {

		private final Map<Class<? extends Option<?, ?>>, ValueContainer<?, ?>> optionValues;

		public ImmutableOptionsContainer(Map<Class<? extends Option<?, ?>>, ValueContainer<?, ?>> optionValues) {
			this.optionValues = Collections.unmodifiableMap( optionValues );
		}

		@Override
		public <I, V> V get(Class<? extends Option<I, V>> optionType, I identifier) {
			ValueContainer<I, V> value = getValueContainer( optionType );
			return value != null ? value.get( identifier ) : null;
		}

		@Override
		public <V> V getUnique(Class<? extends UniqueOption<V>> optionType) {
			ValueContainer<?, V> value = getValueContainer( optionType );
			return value != null ? value.getUnique() : null;
		}

		@Override
		public <I, V, T extends Option<I, V>> Map<I, V> getAll(Class<T> optionType) {
			ValueContainer<I, V> value = getValueContainer( optionType );
			return value != null ? value.getAll() : Collections.<I, V>emptyMap();
		}

		private <V, I> ValueContainer<I, V> getValueContainer(Class<? extends Option<I, V>> optionType) {
			@SuppressWarnings("unchecked")
			ValueContainer<I, V> values = (ValueContainer<I, V>) optionValues.get( optionType );
			return values;
		}

		@Override
		public String toString() {
			return "ImmutableOptionsContainer [optionValues=" + optionValues + "]";
		}
	}
}
