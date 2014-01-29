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
package org.hibernate.ogm.util.configurationreader.impl;

import java.util.List;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * A {@link PropertyReaderContext} which allows to retrieve properties by instantiating a given implementation type,
 * e.g. specified as fully-qualified class name or class object.
 *
 * @author Gunnar Morling
 * @param <T>
 */
public class ClassPropertyReaderContext<T> extends PropertyReaderContext<T> implements ClassPropertyReaderContextExpectingClassLoaderService<T> {

	private static final Log log = LoggerFactory.make();

	private ClassLoaderService classLoaderService;
	private Class<? extends T> defaultImplementation;
	private String defaultImplementationName;
	private Instantiator<T> instantiator;
	private ShortNameResolver shortNameResolver;

	ClassPropertyReaderContext(Object value, String propertyName, Class<T> clazz, T defaultValue, boolean isRequired, List<PropertyValidator<T>> validators) {
		super( value, propertyName, clazz, defaultValue, isRequired, validators );
	}

	/**
	 * Sets the class loader service to be used to load classes by name.
	 */
	@Override
	public ClassPropertyReaderContext<T> withClassLoaderService(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
		return this;
	}

	/**
	 * Sets the default implementation type for the property in case no value is found.
	 */
	public ClassPropertyReaderContext<T> withDefaultImplementation(Class<? extends T> defaultImplementation) {
		this.defaultImplementation = defaultImplementation;
		this.defaultImplementationName = null;
		return this;
	}

	/**
	 * Sets the name of default implementation type for the property in case no value is found.
	 */
	public ClassPropertyReaderContext<T> withDefaultImplementation(String defaultImplementationName) {
		this.defaultImplementationName = defaultImplementationName;
		this.defaultImplementation = null;
		return this;
	}

	/**
	 * Sets an instantiator to be used to create an instance of the property
	 */
	public ClassPropertyReaderContext<T> withInstantiator(Instantiator<T> instantiator) {
		this.instantiator = instantiator;
		return this;
	}

	/**
	 * Sets a short name resolver to be applied in case the property is given as string
	 */
	public ClassPropertyReaderContext<T> withShortNameResolver(ShortNameResolver shortNameResolver) {
		this.shortNameResolver = shortNameResolver;
		return this;
	}

	@Override
	public T getTypedValue() {
		ShortNameResolver resolver = shortNameResolver != null ? shortNameResolver : NoOpNameResolver.INSTANCE;
		Instantiator<T> instantiator = this.instantiator != null ? this.instantiator : DefaultInstantiator.<T>getInstance();

		Object configuredValue = getConfiguredValue();
		Class<T> targetType = getTargetType();

		T typedValue = null;

		if ( configuredValue == null ) {
			typedValue = getDefaultValue( resolver, instantiator );
		}
		else if ( targetType.isAssignableFrom( configuredValue.getClass() ) ) {
			@SuppressWarnings("unchecked")
			T v = (T) configuredValue;
			typedValue = v;
		}
		else if ( configuredValue instanceof Class ) {
			Class<? extends T> configuredClazz = narrowDownClass( (Class<?>) configuredValue, targetType );
			typedValue = instantiator.newInstance( configuredClazz );
		}
		else if ( configuredValue instanceof String ) {
			Class<? extends T> configuredClazz = getClassFromString( (String) configuredValue, targetType, resolver );
			typedValue = instantiator.newInstance( configuredClazz );
		}
		else {
			throw log.unexpectedInstanceType( getPropertyName(), configuredValue.toString(), configuredValue.getClass().getName(), targetType.getName() );
		}

		return typedValue;
	}

	private T getDefaultValue(ShortNameResolver resolver, Instantiator<T> instantiator) {
		if ( getDefaultValue() != null ) {
			return getDefaultValue();
		}
		else if ( defaultImplementationName != null ) {
			defaultImplementation = getClassFromString( defaultImplementationName, getTargetType(), resolver );
		}

		return defaultImplementation != null ? instantiator.newInstance( defaultImplementation ) : null;
	}

	private Class<? extends T> getClassFromString(String className, Class<T> targetType, ShortNameResolver shortNameResolver) {
		if ( shortNameResolver.isShortName( className ) ) {
			className = shortNameResolver.resolve( className );
		}

		Class<?> clazz = null;

		try {
			clazz = classLoaderService.classForName( className );
		}
		catch (ClassLoadingException e) {
			throw log.unableToLoadClass( getPropertyName(), className, e );
		}

		return narrowDownClass( clazz, targetType );
	}

	private Class<? extends T> narrowDownClass(Class<?> clazz, Class<T> targetType) {
		if ( !targetType.isAssignableFrom( clazz ) ) {
			throw log.unexpectedClassType( getPropertyName(), clazz.getName(), targetType.getName() );
		}

		@SuppressWarnings("unchecked")
		Class<T> typed = (Class<T>) clazz;
		return typed;
	}

	private static class NoOpNameResolver implements ShortNameResolver {

		static final NoOpNameResolver INSTANCE = new NoOpNameResolver();

		@Override
		public boolean isShortName(String name) {
			return false;
		}

		@Override
		public String resolve(String shortName) {
			throw new UnsupportedOperationException();
		}
	}

	private static class DefaultInstantiator<T> implements Instantiator<T> {

		@SuppressWarnings("rawtypes")
		private static final DefaultInstantiator<?> INSTANCE = new DefaultInstantiator();

		@SuppressWarnings("unchecked")
		public static <T> DefaultInstantiator<T> getInstance() {
			return (DefaultInstantiator<T>) INSTANCE;
		}

		@Override
		public T newInstance(Class<? extends T> clazz) {
			try {
				return clazz.newInstance();

			}
			catch (Exception e) {
				throw log.unableToInstantiateType( clazz.getName(), e );
			}
		}
	}
}
