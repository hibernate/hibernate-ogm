/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.configurationreader.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.util.configurationreader.spi.ClassPropertyReaderContext;
import org.hibernate.ogm.util.configurationreader.spi.PropertyReaderContext;
import org.hibernate.ogm.util.configurationreader.spi.PropertyValidator;
import org.hibernate.ogm.util.impl.Contracts;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * A context for retrieving the value of a given property.
 *
 * @author Gunnar Morling
 * @param <T> the expected type of the property
 */
abstract class PropertyReaderContextBase<T> implements PropertyReaderContext<T> {

	private static final Log log = LoggerFactory.make();

	private final Object configuredValue;
	private final String propertyName;
	private final Class<T> targetType;
	private final ClassLoaderService classLoaderService;

	private T defaultValue;
	private boolean isRequired;
	private final List<PropertyValidator<T>> validators;

	PropertyReaderContextBase(ClassLoaderService classLoaderService, Object configuredValue, String propertyName, Class<T> targetType) {
		this.classLoaderService = classLoaderService;
		this.configuredValue = configuredValue;
		this.propertyName = propertyName;
		this.targetType = targetType;
		this.validators = new ArrayList<PropertyValidator<T>>();
	}

	PropertyReaderContextBase(ClassLoaderService classLoaderService, Object configuredValue, String propertyName, Class<T> targetType, T defaultValue, boolean isRequired,
			List<PropertyValidator<T>> validators) {
		this.classLoaderService = classLoaderService;
		this.configuredValue = configuredValue;
		this.propertyName = propertyName;
		this.targetType = targetType;

		this.defaultValue = defaultValue;
		this.isRequired = isRequired;
		this.validators = validators;
	}

	@Override
	public PropertyReaderContext<T> withDefault(T defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	@Override
	public PropertyReaderContext<T> required() {
		this.isRequired = true;
		return this;
	}

	@Override
	public PropertyReaderContext<T> withValidator(PropertyValidator<T> validator) {
		validators.add( validator );
		return this;
	}

	@Override
	public ClassPropertyReaderContext<T> instantiate() {
		Contracts.assertNotNull( classLoaderService, "classLoaderService" );
		return new DefaultClassPropertyReaderContext<T>( classLoaderService, configuredValue, propertyName, targetType, defaultValue, isRequired, validators );
	}

	@Override
	public T getValue() {
		if ( isRequired && StringHelper.isNullOrEmptyString( configuredValue ) ) {
			throw log.missingConfigurationProperty( propertyName );
		}

		T typedValue = getTypedValue();

		for ( PropertyValidator<T> validator : validators ) {
			validator.validate( typedValue );
		}

		return typedValue;
	}

	/**
	 * To be implemented in sub-classes to convert the configured value into an object of the property target type.
	 *
	 * @return the configured property as instance of the given target type; May be {@code null}
	 */
	protected abstract T getTypedValue();

	protected Object getConfiguredValue() {
		return configuredValue;
	}

	protected String getPropertyName() {
		return propertyName;
	}

	protected Class<T> getTargetType() {
		return targetType;
	}

	protected T getDefaultValue() {
		return defaultValue;
	}
}
