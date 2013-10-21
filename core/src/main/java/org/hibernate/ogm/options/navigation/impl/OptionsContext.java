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
package org.hibernate.ogm.options.navigation.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;

/**
 * Keeps track of all the options set using one or more invocations of the mapping API; All the options are separated in
 * different contexts: global, per entity and per property. Instances of this class are maintained per session factory
 * and/or per session by {@link org.hibernate.ogm.options.spi.OptionsService}.
 * <p>
 * This class is safe to be accessed from several threads at the same time.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 * @see org.hibernate.ogm.options.spi.OptionsService
 */
public class OptionsContext implements OptionsServiceContext {

	private final OptionsContainer globaloptions = new OptionsContainer();
	private final ConcurrentMap<Class<?>, OptionsContainer> optionsPerEntity = new ConcurrentHashMap<Class<?>, OptionsContainer>();
	private final ConcurrentMap<PropertyKey, OptionsContainer> optionsPerProperty = new ConcurrentHashMap<PropertyKey, OptionsContainer>();

	public void addGlobalOption(Option<?> option) {
		add( option, globaloptions );
	}

	public void addEntityOption(Class<?> entityType, Option<?> option) {
		OptionsContainer entityOptions = optionsPerEntity.get( entityType );

		if ( entityOptions == null ) {
			entityOptions = getAndCacheAnnotationBasedEntityOptions( entityType );
		}

		add( option, entityOptions );
	}

	public void addPropertyOption(Class<?> entityType, String propertyName, Option<?> option) {
		PropertyKey key = new PropertyKey( entityType, propertyName );
		OptionsContainer propertyOptions = optionsPerProperty.get( key );

		if ( propertyOptions == null ) {
			propertyOptions = getAndCacheAnnotationBasedPropertyOptions( key );
		}

		add( option, propertyOptions );
	}

	private void add(Option<?> option, OptionsContainer container) {
		//TODO only needed for SF-scoped context?
		synchronized ( container ) {
			container.add( option );
		}
	}

	@Override
	public OptionsContainer getGlobalOptions() {
		return copy( globaloptions );
	}

	@Override
	public OptionsContainer getEntityOptions(Class<?> entityType) {
		OptionsContainer entityOptions = optionsPerEntity.get( entityType );

		if (entityOptions == null ) {
			entityOptions = getAndCacheAnnotationBasedEntityOptions( entityType );
		}

		return copy( entityOptions );
	}

	@Override
	public OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName) {
		PropertyKey key = new PropertyKey( entityType, propertyName );

		OptionsContainer propertyOptions = optionsPerProperty.get( key );

		if (propertyOptions == null ) {
			propertyOptions = getAndCacheAnnotationBasedPropertyOptions( key );
		}

		return copy( propertyOptions );
	}

	private OptionsContainer copy(OptionsContainer container) {
		//TODO only needed for SF-scoped context?
		synchronized ( container ) {
			return new OptionsContainer( container );
		}
	}

	/**
	 * Retrieves a container with the annotation-based options for the given entity, adding the container to the cache.
	 *
	 * @param entityType the entity type for which to return the options
	 * @return a container with the annotation-based options for the given entity, never {@code null}.
	 */
	private OptionsContainer getAndCacheAnnotationBasedEntityOptions(Class<?> entityType) {
		OptionsContainer entityOptions = AnnotationProcessor.getEntityOptions( entityType );

		OptionsContainer cachedOptions = optionsPerEntity.putIfAbsent( entityType, entityOptions );
		if ( cachedOptions != null ) {
			entityOptions = cachedOptions;
		}

		return entityOptions;
	}

	/**
	 * Retrieves a container with the annotation-based options for the given property, adding the container to the
	 * cache.
	 *
	 * @param key the property for which to return the options
	 * @return a container with the annotation-based options for the given property, never {@code null}.
	 */
	private OptionsContainer getAndCacheAnnotationBasedPropertyOptions(PropertyKey key) {
		Map<PropertyKey, OptionsContainer> allPropertyOptions = AnnotationProcessor.getPropertyOptions( key.getEntity() );

		for ( Entry<PropertyKey, OptionsContainer> option : allPropertyOptions.entrySet() ) {
			optionsPerProperty.putIfAbsent( option.getKey(), option.getValue() );
		}

		OptionsContainer propertyOptions = optionsPerProperty.get( key );

		//cache an empty container in case the given property has no annotation based options
		if ( propertyOptions == null ) {
			propertyOptions = new OptionsContainer();
			OptionsContainer cachedOptions = optionsPerProperty.putIfAbsent( key, propertyOptions );
			if ( cachedOptions != null ) {
				propertyOptions = cachedOptions;
			}
		}

		return propertyOptions;
	}

	@Override
	public String toString() {
		return "OptionsContext [globaloptions=" + globaloptions + ", optionsPerEntity=" + optionsPerEntity + ", optionsPerProperty=" + optionsPerProperty + "]";
	}
}
