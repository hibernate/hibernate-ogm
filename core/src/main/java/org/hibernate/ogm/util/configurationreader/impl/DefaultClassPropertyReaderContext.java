/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.configurationreader.impl;

import java.util.List;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.ogm.util.configurationreader.spi.ClassPropertyReaderContext;
import org.hibernate.ogm.util.configurationreader.spi.PropertyReaderContext;
import org.hibernate.ogm.util.configurationreader.spi.PropertyValidator;
import org.hibernate.ogm.util.configurationreader.spi.ShortNameResolver;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * A {@link PropertyReaderContext} which allows to retrieve properties by instantiating a given implementation type,
 * e.g. specified as fully-qualified class name or class object.
 *
 * @author Gunnar Morling
 * @param <T>
 */
public class DefaultClassPropertyReaderContext<T> extends PropertyReaderContextBase<T> implements ClassPropertyReaderContext<T> {

	private static final Log log = LoggerFactory.make();

	private final ClassLoaderService classLoaderService;
	private Class<? extends T> defaultImplementation;
	private String defaultImplementationName;
	private Instantiator<T> instantiator;
	private ShortNameResolver shortNameResolver;

	DefaultClassPropertyReaderContext(ClassLoaderService classLoaderService, Object value, String propertyName, Class<T> clazz, T defaultValue, boolean isRequired, List<PropertyValidator<T>> validators) {
		super( classLoaderService, value, propertyName, clazz, defaultValue, isRequired, validators );
		this.classLoaderService = classLoaderService;
	}

	@Override
	public DefaultClassPropertyReaderContext<T> withDefaultImplementation(Class<? extends T> defaultImplementation) {
		this.defaultImplementation = defaultImplementation;
		this.defaultImplementationName = null;
		return this;
	}

	@Override
	public DefaultClassPropertyReaderContext<T> withDefaultImplementation(String defaultImplementationName) {
		this.defaultImplementationName = defaultImplementationName;
		this.defaultImplementation = null;
		return this;
	}

	/**
	 * Sets an instantiator to be used to create an instance of the property. Currently not exposed on the SPI as it is
	 * only needed within this module. May be promoted to an SPI later on, if required.
	 */
	public DefaultClassPropertyReaderContext<T> withInstantiator(Instantiator<T> instantiator) {
		this.instantiator = instantiator;
		return this;
	}

	@Override
	public DefaultClassPropertyReaderContext<T> withShortNameResolver(ShortNameResolver shortNameResolver) {
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
