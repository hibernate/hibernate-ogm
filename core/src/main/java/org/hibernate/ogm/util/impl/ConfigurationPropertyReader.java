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
import org.hibernate.cfg.Configuration;

/**
 * Provides a safe access to configuration values as typically configured via
 * {@link org.hibernate.ogm.cfg.OgmConfiguration} or {@code persistence.xml}.
 * <p>
 * Values can be specified in the following representations:
 * <ul>
 * <li>as instance of the expected target type</li>
 * <li>as {@link Class<?>}, representing a sub-type of the expected target type</li>
 * <li>as string, representing the FQN of a sub-type of the expected target type</li>
 * <li>as string, representing a short name as resolvable via a given {@link ShortNameResolver}</li>
 * </ul>
 * If specified as class name, short name or class object, the specified type will be instantiated using its default
 * constructor.
 *
 * @author Gunnar Morling
 */
public class ConfigurationPropertyReader {

	/**
	 * Implementations map short names into fully-qualified class names.
	 *
	 * @author Gunnar Morling
	 */
	public interface ShortNameResolver {

		boolean isShortName(String name);

		String resolve(String shortName);
	}

	/**
	 * Implementations instantiate given classes. By default an implementation invoking the no-args constructor of the
	 * given type is used.
	 *
	 * @author Gunnar Morling
	 */
	public interface Instantiator<T> {

		T newInstance(Class<? extends T> clazz);
	}

	private static class NoOpNameResolver implements ShortNameResolver {

		private static final NoOpNameResolver INSTANCE = new NoOpNameResolver();

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

	/**
	 * A context for retrieving the value of a given property, making several aspects of value retrieval customizable,
	 * such as the instantiation strategy.
	 *
	 * @author Gunnar Morling
	 * @param <T> the expected type of the property
	 */
	public class PropertyReaderContext<T> {

		private final String propertyName;
		private final Class<T> clazz;
		private Class<? extends T> defaultImplementation;
		private String defaultImplementationName;
		private Instantiator<T> instantiator;
		private ShortNameResolver shortNameResolver;

		public PropertyReaderContext(String propertyName, Class<T> clazz) {
			this.propertyName = propertyName;
			this.clazz = clazz;
		}

		/**
		 * Sets the default implementation type for the property in case no value is found.
		 */
		public PropertyReaderContext<T> withDefaultImplementation(Class<? extends T> defaultImplementation) {
			this.defaultImplementation = defaultImplementation;
			this.defaultImplementationName = null;
			return this;
		}

		/**
		 * Sets the name of default implementation type for the property in case no value is found.
		 */
		public PropertyReaderContext<T> withDefaultImplementation(String defaultImplementationName) {
			this.defaultImplementationName = defaultImplementationName;
			this.defaultImplementation = null;
			return this;
		}

		/**
		 * Sets an instantiator to be used to create an instance of the property
		 */
		public PropertyReaderContext<T> withInstantiator(Instantiator<T> instantiator) {
			this.instantiator = instantiator;
			return this;
		}

		/**
		 * Sets a short name resolver to be applied in case the property is given as string
		 */
		public PropertyReaderContext<T> withShortNameResolver(ShortNameResolver shortNameResolver) {
			this.shortNameResolver = shortNameResolver;
			return this;
		}

		/**
		 * Returns the value of the specified property.
		 *
		 * @return the value of the specified property; may be {@code null} in case the property is not present in the
		 * given configuration map and no default implementation has been specified
		 */
		public T getValue() {
			ShortNameResolver resolver = shortNameResolver != null ? shortNameResolver : NoOpNameResolver.INSTANCE;
			Instantiator<T> instantiator = this.instantiator != null ? this.instantiator : DefaultInstantiator.<T>getInstance();

			T value = doGetValue( propertyName, resolver, instantiator, clazz );

			if ( value == null ) {
				if ( defaultImplementationName != null ) {
					defaultImplementation = getClassFromString( propertyName, defaultImplementationName, clazz, resolver );
				}
				if ( defaultImplementation != null ) {
					value = instantiator.newInstance( defaultImplementation );
				}
			}

			return value;
		}
	}

	private static final Log log = LoggerFactory.make();

	private final Map<?, ?> properties;
	private final ClassLoaderService classLoaderService;

	public ConfigurationPropertyReader(Map<?, ?> properties, ClassLoaderService classLoaderService) {
		this.properties = properties;
		this.classLoaderService = classLoaderService;
	}

	public ConfigurationPropertyReader(Configuration configuration, ClassLoaderService classLoaderService) {
		this( configuration.getProperties(), classLoaderService );
	}

	/**
	 * Returns a context for retrieving the specified property. The returned context allows to customize the value
	 * retrieval logic, e.g. by setting a default value or a custom instantiator. Finalze the call by invoking
	 * {@link PropertyReaderContext#getValue()}.
	 *
	 * @param propertyName the name of the property to retrieve
	 * @param targetType the target type of the property
	 * @return a context for retrieving the specified property
	 */
	public <T> PropertyReaderContext<T> property(String propertyName, Class<T> targetType) {
		return new PropertyReaderContext<T>( propertyName, targetType );
	}

	private <T> T doGetValue(String propertyName, ShortNameResolver shortNameResolver, Instantiator<T> instantiator, Class<T> targetType) {
		Object property = properties.get( propertyName );

		if ( property == null ) {
			return null;
		}

		if ( targetType.isAssignableFrom( property.getClass() ) ) {
			@SuppressWarnings("unchecked")
			T value = (T) property;
			return value;
		}

		Class<? extends T> clazz = null;

		if ( property instanceof Class ) {
			clazz = narrowDownClass( propertyName, (Class<?>) property, targetType );
		}
		else if ( property instanceof String ) {
			clazz = getClassFromString( propertyName, (String) property, targetType, shortNameResolver );
		}

		if ( clazz != null ) {
			return instantiator.newInstance( clazz );
		}

		throw log.unexpectedInstanceType( propertyName, property.toString(), property.getClass().getName(), targetType.getName() );
	}

	private <T> Class<? extends T> getClassFromString(String propertyName, String className, Class<T> targetType, ShortNameResolver shortNameResolver) {
		if ( shortNameResolver.isShortName( className ) ) {
			className = shortNameResolver.resolve( className );
		}

		Class<?> clazz = null;

		try {
			clazz = classLoaderService.classForName( className );
		}
		catch (ClassLoadingException e) {
			throw log.unableToLoadClass( propertyName, className, e );
		}

		return narrowDownClass( propertyName, clazz, targetType );
	}

	private <T> Class<? extends T> narrowDownClass(String propertyName, Class<?> clazz, Class<T> targetType) {
		if ( !targetType.isAssignableFrom( clazz ) ) {
			throw log.unexpectedClassType( propertyName, clazz.getName(), targetType.getName() );
		}

		@SuppressWarnings("unchecked")
		Class<T> typed = (Class<T>) clazz;
		return typed;
	}
}
