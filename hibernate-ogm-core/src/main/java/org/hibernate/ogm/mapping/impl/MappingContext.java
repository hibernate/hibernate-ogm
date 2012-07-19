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

	public Map<List<Object>,Object> getProxyPerEntityOrProperty() { return proxyPerEntityOrProperty; }
	private Map<List<Object>,Object> proxyPerEntityOrProperty;

	public Map<Method, Object> getFromInterfaceToGenerator() { return fromInterfaceToGenerator; }
	private Map<Method, Object> fromInterfaceToGenerator;

	public Class<EntityContext<?,?,?>> getEntityContextClass() { return entityContextClass; }
	private Class<EntityContext<?,?,?>> entityContextClass;

	public Class<PropertyContext<?,?,?>> getPropertyContextClass() { return propertyContextClass; }
	private Class<PropertyContext<?,?,?>> propertyContextClass;
}
