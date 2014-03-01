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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
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
public abstract class PropertyReaderContext<T> {

	private static final Log log = LoggerFactory.make();

	private final Object configuredValue;
	private final String propertyName;
	private final Class<T> targetType;
	private final ClassLoaderService classLoaderService;

	private T defaultValue;
	private boolean isRequired;
	private final List<PropertyValidator<T>> validators;

	PropertyReaderContext(ClassLoaderService classLoaderService, Object configuredValue, String propertyName, Class<T> targetType) {
		this.classLoaderService = classLoaderService;
		this.configuredValue = configuredValue;
		this.propertyName = propertyName;
		this.targetType = targetType;
		this.validators = new ArrayList<PropertyValidator<T>>();
	}

	PropertyReaderContext(ClassLoaderService classLoaderService, Object configuredValue, String propertyName, Class<T> targetType, T defaultValue, boolean isRequired,
			List<PropertyValidator<T>> validators) {
		this.classLoaderService = classLoaderService;
		this.configuredValue = configuredValue;
		this.propertyName = propertyName;
		this.targetType = targetType;

		this.defaultValue = defaultValue;
		this.isRequired = isRequired;
		this.validators = validators;
	}

	/**
	 * Sets a default value in case no value is specified for the given property.
	 */
	public PropertyReaderContext<T> withDefault(T defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	/**
	 * Marks the given property as required. In this case an exception will be raised if no value is specified for that
	 * property.
	 */
	public PropertyReaderContext<T> required() {
		this.isRequired = true;
		return this;
	}

	/**
	 * Adds a validator used to validate the value of the given property. Several validators can be added.
	 */
	public PropertyReaderContext<T> withValidator(PropertyValidator<T> validator) {
		validators.add( validator );
		return this;
	}

	/**
	 * Returns a context which allows to specify how the implementation type represented by the given property should be
	 * instantiated.
	 */
	public ClassPropertyReaderContext<T> instantiate() {
		Contracts.assertNotNull( classLoaderService, "classLoaderService" );
		return new ClassPropertyReaderContext<T>( classLoaderService, configuredValue, propertyName, targetType, defaultValue, isRequired, validators );
	}

	/**
	 * Returns the value of the specified property.
	 *
	 * @return the value of the specified property; May be {@code null} in case the property is not present in the given
	 * configuration map and no default implementation has been specified
	 * @throws org.hibernate.HibernateException If the property is marked as required but is not present or if one of the registered
	 * {@link PropertyValidator}s detects an invalid value
	 */
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
