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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.ogm.mapping.annotation.MappingOption;
import org.hibernate.ogm.mapping.context.EntityContext;
import org.hibernate.ogm.mapping.context.GlobalContext;
import org.hibernate.ogm.mapping.context.PropertyContext;
import org.hibernate.ogm.mapping.impl.MappingContext.PropertyKey;
import org.hibernate.ogm.options.AnnotationConverter;
import org.hibernate.ogm.options.EntityOptionsContainer;
import org.hibernate.ogm.options.Option;
import org.hibernate.ogm.options.PropertyOptionsContainer;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Bind the expected generator implementation with the
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MappingToGeneratorInvocationHandler implements InvocationHandler {
	private static final Log log = LoggerFactory.make();

	private static final Method ENTITIES_FROM_GLOBAL;
	private static final Method ENTITIES_FROM_ENTITY;
	private static final Method ENTITIES_FROM_PROPERTY;

	private static final Method PROPERTY_FROM_ENTITY;
	private static final Method PROPERTY_FROM_PROPERTY;

	private final MappingContext context;

	public MappingToGeneratorInvocationHandler(MappingContext context) {
		this.context = context;
	}

	static {
		ENTITIES_FROM_GLOBAL = loadEntityMethod( GlobalContext.class );
		ENTITIES_FROM_ENTITY = loadEntityMethod( EntityContext.class );
		ENTITIES_FROM_PROPERTY = loadEntityMethod( PropertyContext.class );

		PROPERTY_FROM_ENTITY = loadPropertyMethod( EntityContext.class );
		PROPERTY_FROM_PROPERTY = loadPropertyMethod( PropertyContext.class );
	}

	private static Method loadEntityMethod(Class<?> contextClass) {
		return loadMethod( "entity", contextClass, Class.class );
	}

	private static Method loadPropertyMethod(Class<?> contextClass) {
		return loadMethod( "property", contextClass, String.class, ElementType.class );
	}

	private static Method loadMethod(String methodName, Class<?> contextClass, Class<?>... parameterTypes) {
		try {
			return contextClass.getMethod( methodName, parameterTypes );
		}
		catch ( NoSuchMethodException e ) {
			throw log.unableToLoadContext( methodName, contextClass, e );
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// need ArrayList as arrays are compared by == and not by elements identity
		if ( isEqual( method, ENTITIES_FROM_GLOBAL ) || isEqual( method, ENTITIES_FROM_ENTITY )
				|| isEqual( method, ENTITIES_FROM_PROPERTY ) ) {
			return returnProxyForCurrentEntity( method, args );
		}
		else if ( isEqual( method, PROPERTY_FROM_ENTITY ) || isEqual( method, PROPERTY_FROM_PROPERTY ) ) {
			return returnProxyForCurrentProperty( method, args );
		}
		else {
			return returnProxy( proxy, method, args );
		}
	}

	private Object returnProxy(Object proxy, Method method, Object[] args) throws IllegalAccessException,
			InvocationTargetException {
		Object delegate = context.getFromInterfaceToGenerator().get( method );
		if ( delegate != null ) {
			putGeneratorResultInRightContext( method, args, delegate );
		}
		// if nothing match we return the proxy directly?
		// FIXME or should we rather raise an exception?
		return proxy;
	}

	private Object returnProxyForCurrentProperty(Method method, Object[] args) {
		// we could use a typed approach but since we only have entities and properties that will suffice
		// { method, propertyName, elementType }
		List<Object> key = new ArrayList<Object>( 3 );
		key.add( method );
		key.add( args[0] );
		key.add( args[1] );
		// we enter a property context, update the stack
		context.setCurrentProperty( new MappingContext.PropertyKey( context.getCurrentEntity(), (String) args[0] ) );
		return returnProxy( key, context.getPropertyContextClass() );
	}

	private Object returnProxyForCurrentEntity(Method method, Object[] args) {
		// we could use a typed approach but since we only have entities and properties that will suffice
		// { method, propertyName, elemeObjectntType }
		List<Object> key = new ArrayList<Object>( 2 );
		key.add( method );
		key.add( args[0] );
		// We enter an entity context, uaddpdate the stack
		Class<?> entityClass = (Class<?>) args[0];
		context.setCurrentEntity( entityClass );
		context.setCurrentProperty( null );
		addOptionsFromEntityAnnotation( context, context.getCurrentEntity() );
		return returnProxy( key, context.getEntityContextClass() );
	}

	private void putGeneratorResultInRightContext(Method method, Object[] args, Object delegate)
			throws IllegalAccessException, InvocationTargetException {
		Option<?, ?> result = (Option<?, ?>) method.invoke( delegate, args );
		if ( context.getCurrentEntity() == null ) {
			addGlobalOption( result );
		}
		else if ( context.getCurrentProperty() == null ) {
			addEntityOption( result );
		}
		else {
			addPropertyOption( result );
		}
	}

	private void addGlobalOption(Option<?, ?> option) {
		context.getGlobalOptions().add( option );
	}

	private void addPropertyOption(Option<?, ?> option) {
		OptionsContainer container = getOrCreatePropertyOptionContainer( context, context.getCurrentProperty() );
		option.apply( container );
	}

	private void addEntityOption(Option<?, ?> option) {
		OptionsContainer container = getOrCreateEntityOptionContainer( context, context.getCurrentEntity() );
		option.apply( container );
	}

	private OptionsContainer getOrCreatePropertyOptionContainer(MappingContext context, PropertyKey key) {
		OptionsContainer optionsPerProperty = context.getOptionsPerProperty().get( key );
		if ( optionsPerProperty == null ) {
			optionsPerProperty = new PropertyOptionsContainer( key );
			context.getOptionsPerProperty().put( key, optionsPerProperty );
		}
		return optionsPerProperty;
	}

	private OptionsContainer getOrCreateEntityOptionContainer(MappingContext context, Class<?> entityClass) {
		Map<Class<?>, OptionsContainer> options = context.getOptionsPerEntity();
		OptionsContainer optionContainer = options.get( entityClass );
		if ( optionContainer == null ) {
			optionContainer = new EntityOptionsContainer( entityClass );
			options.put( entityClass, optionContainer );
		}
		return optionContainer;
	}

	private void addOptionsFromEntityAnnotation(final MappingContext context, final Class<?> entityClass) {
		Annotation[] annotations = entityClass.getAnnotations();
		saveOptions( annotations, new OptionsContainerLoader() {

			@Override
			public OptionsContainer load() {
				return getOrCreateEntityOptionContainer( context, entityClass );
			}

		} );
		addOptionsFromPropertyAnnotation( context, entityClass );
	}

	private void addOptionsFromPropertyAnnotation(final MappingContext context, final Class<?> entityClass) {
		for ( final Method method : entityClass.getMethods() ) {
			saveOptions( method.getAnnotations(), new OptionsContainerLoader() {

				@Override
				public OptionsContainer load() {
					return getOrCreatePropertyOptionContainer( context, new PropertyKey( entityClass, method.getName() ) );
				}

			} );
		}
		for ( final Field field : entityClass.getFields() ) {
			saveOptions( field.getAnnotations(), new OptionsContainerLoader() {

				@Override
				public OptionsContainer load() {
					return getOrCreatePropertyOptionContainer( context, new PropertyKey( entityClass, field.getName() ) );
				}

			} );
		}
	}

	private void saveOptions(Annotation[] annotations, OptionsContainerLoader containerLoader) {
		for ( Annotation annotation : annotations ) {
			Class<? extends Annotation> class1 = annotation.annotationType();
			Annotation[] qualifiers = class1.getAnnotations();
			for ( Annotation qualifier : qualifiers ) {
				if ( qualifier.annotationType().equals( MappingOption.class ) ) {
					Class<? extends AnnotationConverter> converterClass = ( (MappingOption) qualifier ).value();
					Option option = convert( annotation, converterClass );
					OptionsContainer container = containerLoader.load();
					container.add( option );
					break;
				}
			}
		}
	}

	private Option convert(Annotation annotation, Class<? extends AnnotationConverter> converterClass) {
		try {
			AnnotationConverter converter = converterClass.newInstance();
			return converter.convert( annotation );
		}
		catch ( InstantiationException e ) {
			throw log.cannotConvertAnnotation( converterClass, e );
		}
		catch ( IllegalAccessException e ) {
			throw log.cannotConvertAnnotation( converterClass, e );
		}
	}

	private interface OptionsContainerLoader {
		OptionsContainer load();
	}

	private Object returnProxy(List<Object> key, Class<?> contextClass) {
		Object result = context.getProxyPerEntityOrProperty().get( key );
		if ( result == null ) {
			result = ConfigurationProxyFactory.get( contextClass, context );
			context.getProxyPerEntityOrProperty().put( key, result );
		}
		return result;
	}

	private boolean isEqual(Method from, Method to) {
		return from == to ? true : from.equals( to );
	}
}
