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

import org.hibernate.ogm.mapping.context.EntityContext;
import org.hibernate.ogm.mapping.context.GlobalContext;
import org.hibernate.ogm.mapping.context.PropertyContext;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mapping API context
 * 
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MappingContext {

	public MappingContext(Class<? extends GlobalContext<?, ?, ?>> mappingApi, Set<Object> generators) {
		//TODO technically this could/should be done beforehand to keep a single Map across all calls
		this.fromInterfaceToGenerator = new HashMap<Method, Object>();
		for ( Object generator : generators ) {
			Class<?> generatorClass = generator.getClass();
			enlistMethods( generator, generatorClass );
		}
		proxyPerEntityOrProperty = new HashMap<List<Object>, Object>();
		globalOptions = new ArrayList<Object>();
		optionsPerEntity = new HashMap<Class<?>, List<Object>>(  );
		optionsPerProperty = new HashMap<PropertyKey, List<Object>>(  );
		processGlobalContextType( mappingApi );
	}

	private void enlistMethods(Object generator, Class<?> generatorClass) {
		if ( generatorClass.isInterface() ) {
			for ( Method method : generatorClass.getMethods() ) {
				fromInterfaceToGenerator.put( method, generator );
			}
		}
		for ( Class<?> clazz : generatorClass.getInterfaces() ) {
			enlistMethods( generator, clazz );
		}
	}

	private void processGlobalContextType(Class<? extends GlobalContext<?,?,?>> globalContextSubtype) {
		Map<Type,Type> correspondingTypes = new HashMap<Type, Type>();
		Type globalContextType = processInterfaces(globalContextSubtype, correspondingTypes);
		Type[] typeArguments;
		if ( globalContextType instanceof Class ) {
			typeArguments = ( (Class<?>) globalContextType ).getTypeParameters();
		}
		else {
			typeArguments = ( (ParameterizedType) globalContextType ).getActualTypeArguments();
		}
		Type entityContext = typeArguments[1];
		this.entityContextClass = (Class<EntityContext<?,?,?>>) resolveType(entityContext, correspondingTypes);
		Type propertyContext = typeArguments[2];
		this.propertyContextClass = (Class<PropertyContext<?,?,?>>) resolveType(propertyContext, correspondingTypes);
	}

	private Class<?> resolveType(Type context, Map<Type,Type> correspondingTypes) {
		Type finalType;
		Type correspondingType = context;
		do {
			finalType = correspondingType;
			correspondingType = correspondingTypes.get( finalType );
		}
		while ( correspondingType != null );
		return getClassFromType( finalType );
	}

	private Type processInterfaces(Type globalContextSubtype, Map<Type, Type> correspondingTypes) {
		Class<?> clazz = getClassFromType( globalContextSubtype );
		if ( GlobalContext.class.equals( clazz ) ) {
			return globalContextSubtype;
		}
		else if ( GlobalContext.class.isAssignableFrom( clazz ) ) {
			Type[] interfaces;
			if ( globalContextSubtype instanceof Class ) {
				interfaces = ( (Class<?>) globalContextSubtype ).getGenericInterfaces();
			}
			else {
				ParameterizedType paramType = (ParameterizedType) globalContextSubtype;
				Type rawType = paramType.getRawType();
				Class<?> rawClassFromType = getClassFromType( rawType );
				//populate corresponding type map
				Type[] actualTypeArguments = paramType.getActualTypeArguments();
				Type[] typeParameters = rawClassFromType.getTypeParameters();
				for ( int index = 0 ; index < actualTypeArguments.length ; index++ ) {
					correspondingTypes.put( typeParameters[index], actualTypeArguments[index] );
				}
				//stop type or continue
				if ( GlobalContext.class.equals( rawClassFromType ) ) {
					return rawType;
				}
				else {
					interfaces = rawClassFromType.getGenericInterfaces();
				}
			}
			for ( Type type : interfaces ) {
				Type match = processInterfaces( type, correspondingTypes );
				if ( match != null ) {
					return match;
				}
			}
			return null; //should never happen as we know we are a subclass of GlobalContext
		}
		else {
			return null; //not the right hierarchy
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
			//not sure what to do here but we won't be in this situation I think
			return null;
		}
		else if ( type instanceof WildcardType ) {
			//Not sure what to do here
			return null;
		}
		else {
			return null;
		}
	}

	/** cache of already built proxy instances */
	public Map<List<Object>,Object> getProxyPerEntityOrProperty() { return proxyPerEntityOrProperty; }
	private Map<List<Object>,Object> proxyPerEntityOrProperty;

	/** cache of appropriate generator instance per expected method */
	public Map<Method, Object> getFromInterfaceToGenerator() { return fromInterfaceToGenerator; }
	private Map<Method, Object> fromInterfaceToGenerator;

	/** Actual subclass instance of EntityContext detected from concrete entry point type (GlobalContext subclass) */
	public Class<EntityContext<?,?,?>> getEntityContextClass() { return entityContextClass; }
	private Class<EntityContext<?,?,?>> entityContextClass;

	/** Actual subclass instance of PropertyContext detected from concrete entry point type (GlobalContext subclass) */
	public Class<PropertyContext<?,?,?>> getPropertyContextClass() { return propertyContextClass; }
	private Class<PropertyContext<?,?,?>> propertyContextClass;

	/** Gather the internal model of the global options */
	public List<Object> getGlobalOptions() { return globalOptions; }
	public void setGlobalOptions(List<Object> globalOptions) {  this.globalOptions = globalOptions; }
	private List<Object> globalOptions;

	/** Gather the internal model of the entity level options */
	public Map<Class<?>,List<Object>> getOptionsPerEntity() { return optionsPerEntity; }
	public void setOptionsPerEntity(Map<Class<?>,List<Object>> optionsPerEntity) {  this.optionsPerEntity = optionsPerEntity; }
	private Map<Class<?>,List<Object>>  optionsPerEntity;

	/** Gather the internal model of the property level options */
	public Map<PropertyKey,List<Object>> getOptionsPerProperty() { return optionsPerProperty; }
	private Map<PropertyKey,List<Object>> optionsPerProperty;

	/** Returns the current entity being processed or null of at the global level */
	public Class<?> getCurrentEntity() { return currentEntity; }
	public void setCurrentEntity(Class<?> currentEntity) {  this.currentEntity = currentEntity; }
	private Class<?> currentEntity;

	/** Returns the current property being processed or null of at the global or entity level */
	public PropertyKey getCurrentProperty() { return currentProperty; }
	public void setCurrentProperty(PropertyKey currentProperty) {  this.currentProperty = currentProperty; }
	private PropertyKey currentProperty;

	/**
	 * Represents the lookup key to uniquely identify a property
	 */
	public static class PropertyKey {
		public PropertyKey(Class<?> entity, String property) {
			this.entity = entity;
			this.property = property;
		}

		public Class<?> getEntity() { return entity; }
		public void setEntity(Class<?> entity) {  this.entity = entity; }
		private Class<?> entity;

		public String getProperty() { return property; }
		public void setProperty(String property) {  this.property = property; }
		private String property;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) return true;
			if ( o == null || getClass() != o.getClass() ) return false;

			PropertyKey that = (PropertyKey) o;

			if ( !entity.equals( that.entity ) ) return false;
			if ( !property.equals( that.property ) ) return false;

			return true;
		}

		@Override
		public int hashCode() {
			int result = entity.hashCode();
			result = 31 * result + property.hashCode();
			return result;
		}
	}
}
