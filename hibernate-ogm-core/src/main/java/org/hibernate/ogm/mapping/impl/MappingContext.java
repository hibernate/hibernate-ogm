/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.mapping.impl;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.mapping.context.EntityContext;
import org.hibernate.ogm.mapping.context.GlobalContext;
import org.hibernate.ogm.mapping.context.PropertyContext;
import org.hibernate.ogm.options.GlobalOptionsContainer;

/**
 * Mapping API context
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MappingContext {

	private final Map<Method, Object> fromInterfaceToGenerator;

	private Map<List<Object>, Object> proxyPerEntityOrProperty;

	private Class<?> entityContextClass;

	private Class<?> propertyContextClass;

	private final OptionsContainer globalOptions;

	private final Map<Class<?>, OptionsContainer> optionsPerEntity;

	private final Map<PropertyKey, OptionsContainer> optionsPerProperty;

	private Class<?> currentEntity;

	private PropertyKey currentProperty;

	public <G extends GlobalContext<G,E,P>, E extends EntityContext<G,E,P>, P extends PropertyContext<G,E,P>> MappingContext(Class<G> mappingApi, Set<Object> generators) {
		this.fromInterfaceToGenerator = fromInterfaceToGenerator( generators );
		this.proxyPerEntityOrProperty = new HashMap<List<Object>, Object>();
		this.globalOptions = new GlobalOptionsContainer();

		this.optionsPerEntity = new HashMap<Class<?>, OptionsContainer>();
		this.optionsPerProperty = new HashMap<PropertyKey, OptionsContainer>();
		assignGlobalContextTypes( mappingApi );
	}

	private Map<Method, Object> fromInterfaceToGenerator(Set<Object> generators) {
		// TODO technically this could/should be done beforehand to keep a single Map across all calls
		Map<Method, Object> fromInterfaceToGenerator = new HashMap<Method, Object>();
		for ( Object generator : generators ) {
			Class<?> generatorClass = generator.getClass();
			enlistMethods( fromInterfaceToGenerator, generator, generatorClass );
		}
		return fromInterfaceToGenerator;
	}

	private void enlistMethods(Map<Method, Object> fromInterfaceToGenerator, Object generator, Class<?> generatorClass) {
		if ( generatorClass.isInterface() ) {
			for ( Method method : generatorClass.getMethods() ) {
				fromInterfaceToGenerator.put( method, generator );
			}
		}
		for ( Class<?> clazz : generatorClass.getInterfaces() ) {
			enlistMethods( fromInterfaceToGenerator, generator, clazz );
		}
	}

	private void assignGlobalContextTypes(Class<?> globalContextSubtype) {
		Map<Type, Type> correspondingTypes = new HashMap<Type, Type>();
		Type globalContextType = process( globalContextSubtype, correspondingTypes );
		Type[] typeArguments = typeArguments( globalContextType );

		this.entityContextClass = (Class<?>) resolveType( typeArguments[1], correspondingTypes );
		this.propertyContextClass = (Class<?>) resolveType( typeArguments[2], correspondingTypes );
	}

	private Type process(Class<?> globalContextSubtype, Map<Type, Type> correspondingTypes) {
		Class<?> clazz = getClassFromType( globalContextSubtype );
		if ( GlobalContext.class.equals( clazz ) ) {
			return globalContextSubtype;
		}
		else if ( GlobalContext.class.isAssignableFrom( clazz ) ) {
			return processSubClass( globalContextSubtype, correspondingTypes );
		}
		else {
			// not the right hierarchy
			return null;
		}
	}

	private Type[] typeArguments(Type globalContextType) {
		if ( globalContextType instanceof Class ) {
			return ( (Class<?>) globalContextType ).getTypeParameters();
		}
		else {
			return ( (ParameterizedType) globalContextType ).getActualTypeArguments();
		}
	}

	private Class<?> resolveType(Type context, Map<Type, Type> correspondingTypes) {
		Type finalType;
		Type correspondingType = context;
		do {
			finalType = correspondingType;
			correspondingType = correspondingTypes.get( finalType );
		} while ( correspondingType != null );
		return getClassFromType( finalType );
	}

	private Type processInterfaces(Type globalContextSubtype, Map<Type, Type> correspondingTypes) {
		Class<?> clazz = getClassFromType( globalContextSubtype );
		if ( GlobalContext.class.equals( clazz ) ) {
			return globalContextSubtype;
		}
		else if ( GlobalContext.class.isAssignableFrom( clazz ) ) {
			return processSubClass( globalContextSubtype, correspondingTypes );
		}
		else {
			// not the right hierarchy
			return null;
		}
	}

	private Type processSubClass(Type globalContextSubtype, Map<Type, Type> correspondingTypes) {
		if ( globalContextSubtype instanceof Class ) {
			Type[] interfaces = ( (Class<?>) globalContextSubtype ).getGenericInterfaces();
			return findType( correspondingTypes, interfaces );
		}
		else {
			ParameterizedType paramType = (ParameterizedType) globalContextSubtype;
			Class<?> rawClassFromType = getClassFromType( paramType.getRawType() );
			populateCorrespondingTypes( correspondingTypes, paramType, rawClassFromType.getTypeParameters() );

			// stop type or continue
			if ( GlobalContext.class.equals( rawClassFromType ) ) {
				return paramType.getRawType();
			}
			else {
				Type[] interfaces = rawClassFromType.getGenericInterfaces();
				return findType( correspondingTypes, interfaces );
			}
		}
	}

	private Type findType(Map<Type, Type> correspondingTypes, Type[] interfaces) {
		for ( Type type : interfaces ) {
			Type match = processInterfaces( type, correspondingTypes );
			if ( match != null ) {
				return match;
			}
		}
		// should never happen as we know we are a subclass of GlobalContext
		return null;
	}

	private void populateCorrespondingTypes(Map<Type, Type> correspondingTypes, ParameterizedType paramType,
			Type[] typeParameters) {
		Type[] actualTypeArguments = paramType.getActualTypeArguments();
		for ( int index = 0; index < actualTypeArguments.length; index++ ) {
			correspondingTypes.put( typeParameters[index], actualTypeArguments[index] );
		}
	}

	private Class<?> getClassFromType(Type type) {
		if ( type instanceof Class ) {
			return (Class<?>) type;
		}
		else if ( type instanceof ParameterizedType ) {
			return getClassFromType( ( (ParameterizedType) type ).getRawType() );
		}
		else if ( type instanceof GenericArrayType ) {
			// not sure what to do here but we won't be in this situation I think
			return null;
		}
		else if ( type instanceof WildcardType ) {
			// Not sure what to do here
			return null;
		}
		else {
			return null;
		}
	}

	/** cache of already built proxy instances */
	public Map<List<Object>, Object> getProxyPerEntityOrProperty() {
		return proxyPerEntityOrProperty;
	}

	/** cache of appropriate generator instance per expected method */
	public Map<Method, Object> getFromInterfaceToGenerator() {
		return fromInterfaceToGenerator;
	}

	/** Actual subclass instance of EntityContext detected from concrete entry point type (GlobalContext subclass) */
	public Class<?> getEntityContextClass() {
		return entityContextClass;
	}

	/** Actual subclass instance of PropertyContext detected from concrete entry point type (GlobalContext subclass) */
	public Class<?> getPropertyContextClass() {
		return propertyContextClass;
	}

	/** Gather the internal model of the global options */
	public OptionsContainer getGlobalOptions() {
		return globalOptions;
	}

	public Map<Class<?>, OptionsContainer> getOptionsPerEntity() {
		return optionsPerEntity;
	}

	/** Gather the internal model of the property level options */
	public Map<PropertyKey, OptionsContainer> getOptionsPerProperty() {
		return optionsPerProperty;
	}

	/** Returns the current entity being processed or null of at the global level */
	public Class<?> getCurrentEntity() {
		return currentEntity;
	}

	public void setCurrentEntity(Class<?> currentEntity) {
		this.currentEntity = currentEntity;
	}

	/** Returns the current property being processed or null of at the global or entity level */
	public PropertyKey getCurrentProperty() {
		return currentProperty;
	}

	public void setCurrentProperty(PropertyKey currentProperty) {
		this.currentProperty = currentProperty;
	}

	/**
	 * Represents the lookup key to uniquely identify a property
	 */
	public static class PropertyKey {
		private final Class<?> entity;

		private final String property;

		public PropertyKey(Class<?> entity, String property ) {
			this.entity = entity;
			this.property = property;
		}

		public Class<?> getEntity() {
			return entity;
		}

		public String getProperty() {
			return property;
		}

		@Override
		public String toString() {
			return entity + ": " + property;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( entity == null ) ? 0 : entity.hashCode() );
			result = prime * result + ( ( property == null ) ? 0 : property.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			PropertyKey other = (PropertyKey) obj;
			if ( entity == null ) {
				if ( other.entity != null ) {
					return false;
				}
			}
			else {
				if ( !entity.equals( other.entity ) ) {
					return false;
				}
			}
			if ( property == null ) {
				if ( other.property != null ) {
					return false;
				}
			}
			else {
				if ( !property.equals( other.property ) ) {
					return false;
				}
			}
			return true;
		}

	}

}
