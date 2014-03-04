/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
