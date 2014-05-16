/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newConcurrentHashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.options.spi.UniqueOption;

/**
 * Provides access to the options effectively applying for a given entity or property.
 *
 * @author Gunnar Morling
 */
public class OptionsContextImpl implements OptionsContext {

	private final List<OptionValueSource> sources;
	private final Class<?> entityType;
	private final String propertyName;
	private final List<Class<?>> hierarchy;

	/**
	 * Caches the container contributing the value per option type.
	 */
	private final ConcurrentMap<Class<? extends Option<?, ?>>, OptionsContainer> optionCache;

	private OptionsContextImpl(List<OptionValueSource> sources, Class<?> entityType, String propertyName) {
		this.sources = sources;
		this.entityType = entityType;
		this.propertyName = propertyName;
		this.hierarchy = getClassHierarchy( entityType );
		this.optionCache = newConcurrentHashMap();
	}

	public static OptionsContext forGlobal(List<OptionValueSource> sources) {
		return new OptionsContextImpl( sources, null, null );
	}

	public static OptionsContext forProperty(List<OptionValueSource> sources, Class<?> entityType, String propertyName) {
		return new OptionsContextImpl( sources, entityType, propertyName );
	}

	public static OptionsContext forEntity(List<OptionValueSource> sources, Class<?> entityType) {
		return new OptionsContextImpl( sources, entityType, null );
	}

	@Override
	public <I, V, O extends Option<I, V>> V get(Class<O> optionType, I identifier) {
		OptionsContainer optionsContainer = optionCache.get( optionType );

		if ( optionsContainer == null ) {
			optionsContainer = getAndCacheOptionsContainer( optionType );
		}

		return optionsContainer.get( optionType, identifier );
	}

	@Override
	public <V, O extends UniqueOption<V>> V getUnique(Class<O> optionType) {
		OptionsContainer optionsContainer = optionCache.get( optionType );

		if ( optionsContainer == null ) {
			optionsContainer = getAndCacheOptionsContainer( optionType );
		}

		return optionsContainer.getUnique( optionType );
	}

	@Override
	public <I, V, O extends Option<I, V>> Map<I, V> getAll(Class<O> optionType) {
		OptionsContainer optionsContainer = optionCache.get( optionType );

		if ( optionsContainer == null ) {
			optionsContainer = getAndCacheOptionsContainer( optionType );
		}

		return optionsContainer.getAll( optionType );
	}

	private <I, V, O extends Option<I, V>> OptionsContainer getAndCacheOptionsContainer(Class<O> optionType) {
		OptionsContainer container = getMostSpecificContainer( optionType );

		OptionsContainer cachedContainer = optionCache.putIfAbsent( optionType, container );
		if ( cachedContainer != null ) {
			container = cachedContainer;
		}

		return container;
	}

	/**
	 * Returns that container which effectively contributes the given option's value as per the algorithm outlined in
	 * the documentation of {@link OptionsContext}.
	 *
	 * @param optionType the option type of interest
	 * @return the container which effectively contributes the given option's value; May be an empty container in case
	 * no value at all is configured for the given option, but never {@code null}
	 */
	private <I, V, O extends Option<I, V>> OptionsContainer getMostSpecificContainer(Class<O> optionType) {
		OptionsContainer container;

		if ( propertyName != null ) {
			for ( Class<?> clazz : hierarchy ) {
				for ( OptionValueSource source : sources ) {
					container = source.getPropertyOptions( clazz, propertyName );
					if ( !container.getAll( optionType ).isEmpty() ) {
						return container;
					}
				}
			}
		}

		if ( entityType != null ) {
			for ( Class<?> clazz : hierarchy ) {
				for ( OptionValueSource source : sources ) {
					container = source.getEntityOptions( clazz );
					if ( !container.getAll( optionType ).isEmpty() ) {
						return container;
					}
				}
			}
		}

		for ( OptionValueSource source : sources ) {
			container = source.getGlobalOptions();
			if ( !container.getAll( optionType ).isEmpty() ) {
				return container;
			}
		}

		return OptionsContainer.EMPTY;
	}

	/**
	 * Returns the class hierarchy of the given type, from bottom to top, starting with the given class itself.
	 * Interfaces are not included.
	 *
	 * @param clazz the class of interest
	 * @return the class hierarchy of the given class
	 */
	private static List<Class<?>> getClassHierarchy(Class<?> clazz) {
		List<Class<?>> hierarchy = new ArrayList<Class<?>>( 4 );

		for ( Class<?> current = clazz; current != null; current = current.getSuperclass() ) {
			hierarchy.add( current );
		}

		return hierarchy;
	}

	@Override
	public String toString() {
		return "OptionsContextImpl [entityType=" + entityType + ", propertyName=" + propertyName + "]";
	}
}
