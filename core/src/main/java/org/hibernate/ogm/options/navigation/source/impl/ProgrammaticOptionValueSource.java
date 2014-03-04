/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.options.navigation.source.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.ogm.options.navigation.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.impl.PropertyKey;
import org.hibernate.ogm.options.spi.Option;

/**
 * A {@link OptionValueSource} which provides access to options set via the programmatic option API.
 * <p>
 * This class is safe to be accessed from several threads at the same time.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 * @see org.hibernate.ogm.options.spi.OptionsService
 */
public class ProgrammaticOptionValueSource implements OptionValueSource {

	private final OptionsContainer globalOptions = new OptionsContainer();
	private final ConcurrentMap<Class<?>, OptionsContainer> optionsPerEntity = new ConcurrentHashMap<Class<?>, OptionsContainer>();
	private final ConcurrentMap<PropertyKey, OptionsContainer> optionsPerProperty = new ConcurrentHashMap<PropertyKey, OptionsContainer>();

	public <V> void addGlobalOption(Option<?, V> option, V value) {
		globalOptions.add( option, value );
	}

	public <V> void addEntityOption(Class<?> entityType, Option<?, V> option, V value) {
		OptionsContainer entityOptions = optionsPerEntity.get( entityType );

		if ( entityOptions == null ) {
			entityOptions = new OptionsContainer();
			optionsPerEntity.put( entityType, entityOptions );
		}

		entityOptions.add( option, value );
	}

	public <V> void addPropertyOption(Class<?> entityType, String propertyName, Option<?, V> option, V value) {
		PropertyKey key = new PropertyKey( entityType, propertyName );
		OptionsContainer propertyOptions = optionsPerProperty.get( key );

		if ( propertyOptions == null ) {
			propertyOptions = new OptionsContainer();
			optionsPerProperty.put( key, propertyOptions );
		}

		propertyOptions.add( option, value );
	}

	@Override
	public OptionsContainer getGlobalOptions() {
		return globalOptions;
	}

	@Override
	public OptionsContainer getEntityOptions(Class<?> entityType) {
		OptionsContainer entityOptions = optionsPerEntity.get( entityType );
		return entityOptions != null ? entityOptions : OptionsContainer.EMPTY;
	}

	@Override
	public OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName) {
		OptionsContainer propertyOptions = optionsPerProperty.get( new PropertyKey( entityType, propertyName ) );
		return propertyOptions != null ? propertyOptions : OptionsContainer.EMPTY;
	}
}
