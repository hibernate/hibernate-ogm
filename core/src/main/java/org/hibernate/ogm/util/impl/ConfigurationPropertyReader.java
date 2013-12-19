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
package org.hibernate.ogm.util.impl;

import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Provides a safe access to configuration values as typically configured via
 * {@link org.hibernate.ogm.cfg.OgmConfiguration} or {@code persistence.xml}.
 *
 * @author Gunnar Morling
 */
public class ConfigurationPropertyReader {

	private static final Log log = LoggerFactory.make();

	private final Map<?, ?> properties;
	private final ServiceRegistryImplementor registry;

	public ConfigurationPropertyReader(Map<?, ?> properties, ServiceRegistryImplementor registry) {
		this.properties = properties;
		this.registry = registry;
	}

	/**
	 * Retrieves the value of the specified property. Values can be specified in the following representations:
	 * <ul>
	 * <li>as instance of the expected target type</li>
	 * <li>as string, representing the FQN of a sub-type of the expected target type</li>
	 * <li>as {@link Class<?>}, representing a sub-type of the expected target type</li>
	 * </ul>
	 * If specified as class name or class object, the specified type will be instantiated using its default
	 * constructor.
	 *
	 * @param propertyName the name of the property to retrieve
	 * @param targetType the target type of the property
	 * @return the value of the specified property or {@code null} if the property is not present
	 */
	public <T> T getValue(String propertyName, Class<T> targetType) {
		Object property = properties.get( propertyName );

		if ( property == null ) {
			return null;
		}

		// instance
		if ( targetType.isAssignableFrom( property.getClass() ) ) {
			@SuppressWarnings("unchecked")
			T value = (T) property;
			return value;
		}

		// class, either as Class object or FQN
		Class<T> clazz = asClass( propertyName, property, targetType );

		if ( clazz != null ) {
			return newInstance( clazz );
		}

		throw log.unexpectedInstanceType( propertyName, property.toString(), property.getClass().getName(), targetType.getName() );
	}

	private <T> Class<T> asClass(String propertyName, Object property, Class<T> targetType) {
		Class<?> clazz = null;

		// class
		if ( property instanceof Class ) {
			clazz = (Class<?>) property;
		}
		// FQN
		else if ( property instanceof String ) {
			try {
				clazz = registry.getService( ClassLoaderService.class ).classForName( (String) property );
			}
			catch (ClassLoadingException e) {
				throw log.unableToLoadClass( propertyName, (String) property, e );
			}
		}

		if ( clazz == null ) {
			return null;
		}
		if ( !targetType.isAssignableFrom( clazz ) ) {
			throw log.unexpectedClassType( propertyName, clazz.getName(), targetType.getName() );
		}

		@SuppressWarnings("unchecked")
		Class<T> typed = (Class<T>) clazz;
		return typed;
	}

	private <T> T newInstance(Class<T> type) {
		try {
			return type.newInstance();

		}
		catch (Exception e) {
			throw log.unableToInstantiateType( type.getName(), e );
		}
	}
}
