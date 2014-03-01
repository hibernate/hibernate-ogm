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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * A {@link PropertyReaderContext} which allows to retrieve {@code String}, {@code int}, {@code boolean}, {@code enum}
 * and {@link URL} properties.
 *
 * @author Gunnar Morling
 * @param <T> The type of the property to retrieve
 */
class SimplePropertyReaderContext<T> extends PropertyReaderContext<T> {

	private static final Log log = LoggerFactory.make();

	SimplePropertyReaderContext(ClassLoaderService classLoaderService, Map<?, ?> configurationValues, String propertyName, Class<T> clazz) {
		super( classLoaderService, configurationValues.get( propertyName ), propertyName, clazz );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T getTypedValue() {
		T typedValue = null;
		Class<T> targetType = getTargetType();

		if ( targetType == String.class ) {
			typedValue = (T) getAsString();
		}
		else if ( targetType == boolean.class ) {
			typedValue = (T) getAsBoolean();
		}
		else if ( targetType == int.class ) {
			typedValue = (T) getAsInt();
		}
		else if ( targetType.isEnum() ) {
			typedValue = (T) getAsEnum();
		}
		else if ( targetType == URL.class ) {
			typedValue = (T) getAsUrl();
		}
		else {
			throw log.unsupportedPropertyType( getPropertyName(), getConfiguredValue().toString() );
		}
		return typedValue;
	}

	private String getAsString() {
		String stringValue = StringHelper.isNullOrEmptyString( getConfiguredValue() ) ? null : getConfiguredValue().toString().trim();
		return stringValue == null ? (String) getDefaultValue() : stringValue;
	}

	private Boolean getAsBoolean() {
		Object configuredValue = getConfiguredValue();

		if ( StringHelper.isNullOrEmptyString( configuredValue ) ) {
			return getDefaultValue() != null ? (Boolean) getDefaultValue() : false;
		}

		return ( configuredValue instanceof Boolean ) ? (Boolean) configuredValue : Boolean.valueOf( configuredValue.toString().trim() );
	}

	private Integer getAsInt() {
		Object configuredValue = getConfiguredValue();

		if ( StringHelper.isNullOrEmptyString( configuredValue ) ) {
			return getDefaultValue() != null ? (Integer) getDefaultValue() : 0;
		}
		else if ( configuredValue instanceof Number ) {
			return ( (Number) configuredValue ).intValue();
		}
		else {
			try {
				String stringValue = configuredValue.toString().trim();
				return Integer.valueOf( stringValue );
			}
			catch (NumberFormatException e) {
				throw log.notAnInteger( getPropertyName(), configuredValue.toString() );
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <E extends Enum<E>> E getAsEnum() {
		Object configuredValue = getConfiguredValue();
		Class<? extends Object> targetType = getTargetType();

		if ( StringHelper.isNullOrEmptyString( configuredValue ) ) {
			return (E) getDefaultValue();
		}
		else if ( configuredValue.getClass() == targetType ) {
			E asEnum = (E) configuredValue;
			return asEnum;
		}
		else {
			try {
				String stringValue = configuredValue.toString().trim().toUpperCase( Locale.ENGLISH );
				return Enum.valueOf( (Class<E>) targetType, stringValue );
			}
			catch (IllegalArgumentException e) {
				throw log.unknownEnumerationValue( getPropertyName(), configuredValue.toString(), Arrays.toString( targetType.getEnumConstants() ) );
			}
		}
	}

	private URL getAsUrl() {
		Object configuredValue = getConfiguredValue();

		if ( StringHelper.isNullOrEmptyString( configuredValue ) ) {
			return (URL) getDefaultValue();
		}
		else if ( configuredValue instanceof URL ) {
			return (URL) configuredValue;
		}
		else {
			String stringValue = configuredValue.toString().trim();

			URL resource = getFromClassPath( stringValue );

			if ( resource == null ) {
				resource = getFromStringUrl( stringValue );
			}
			if ( resource == null ) {
				resource = getFromFileSystemPath( stringValue );
			}
			if ( resource == null ) {
				throw log.invalidConfigurationUrl( getPropertyName(), configuredValue.toString() );
			}

			return resource;
		}
	}

	private URL getFromFileSystemPath(String stringValue) {
		File file = new File( stringValue );

		if ( !file.exists() ) {
			return null;
		}
		else {
			try {
				return file.toURI().toURL();
			}
			catch (MalformedURLException e) {
				// ignore
				return null;
			}
		}
	}

	private URL getFromClassPath(String stringValue) {
		return Thread.currentThread().getContextClassLoader().getResource( stringValue );
	}

	private URL getFromStringUrl(String stringValue) {
		try {
			return new URL( stringValue );
		}
		catch (MalformedURLException e) {
			// ignore
			return null;
		}
	}
}
