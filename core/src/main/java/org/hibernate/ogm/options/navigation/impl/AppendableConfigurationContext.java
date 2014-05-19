/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.impl;

import static org.hibernate.ogm.util.impl.CollectionHelper.newConcurrentHashMap;

import java.util.Map;

import org.hibernate.ogm.options.container.impl.OptionsContainerBuilder;
import org.hibernate.ogm.options.spi.Option;

/**
 * A mutable context managing programmatically set option values.
 *
 * @author Gunnar Morling
 */
public class AppendableConfigurationContext {

	private final OptionsContainerBuilder globalOptions = new OptionsContainerBuilder();
	private final Map<Class<?>, OptionsContainerBuilder> optionsPerEntity = newConcurrentHashMap();
	private final Map<PropertyKey, OptionsContainerBuilder> optionsPerProperty = newConcurrentHashMap();

	public <V> void addGlobalOption(Option<?, V> option, V value) {
		globalOptions.add( option, value );
	}

	public <V> void addEntityOption(Class<?> entityType, Option<?, V> option, V value) {
		OptionsContainerBuilder entityOptions = optionsPerEntity.get( entityType );

		if ( entityOptions == null ) {
			entityOptions = new OptionsContainerBuilder();
			optionsPerEntity.put( entityType, entityOptions );
		}

		entityOptions.add( option, value );
	}

	public <V> void addPropertyOption(Class<?> entityType, String propertyName, Option<?, V> option, V value) {
		PropertyKey key = new PropertyKey( entityType, propertyName );
		OptionsContainerBuilder propertyOptions = optionsPerProperty.get( key );

		if ( propertyOptions == null ) {
			propertyOptions = new OptionsContainerBuilder();
			optionsPerProperty.put( key, propertyOptions );
		}

		propertyOptions.add( option, value );
	}

	public OptionsContainerBuilder getGlobalOptions() {
		return globalOptions;
	}

	public Map<Class<?>, OptionsContainerBuilder> getEntityOptions() {
		return optionsPerEntity;
	}

	public Map<PropertyKey, OptionsContainerBuilder> getPropertyOptions() {
		return optionsPerProperty;
	}
}
