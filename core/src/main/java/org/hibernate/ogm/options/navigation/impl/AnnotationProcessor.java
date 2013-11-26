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
package org.hibernate.ogm.options.navigation.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.MappingOption;
import org.hibernate.ogm.options.spi.OptionValuePair;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.ReflectionHelper;

/**
 * Reads the option annotations on an entity and save them in the appropriate context.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public class AnnotationProcessor {

	private static final Log log = LoggerFactory.make();

	/**
	 * Returns an {@link OptionsContainer} with the entity-level options of the given type.
	 *
	 * @param entityType class of the entity annotated with the options
	 * @return an {@link OptionsContainer} with the entity-level options of the given type; may be empty but never
	 * {@code null}
	 */
	public static OptionsContainer getEntityOptions(Class<?> entityType) {
		return convertOptionAnnotations( entityType.getAnnotations() );
	}

	/**
	 * Returns an {@link OptionsContainer} with the property-level options of the given type, keyed by property.
	 *
	 * @param entityClass class containing the option annotation
	 * @return an {@link OptionsContainer} with the property-level options of the given type; may be empty but never
	 * {@code null}
	 */
	public static Map<PropertyKey, OptionsContainer> getPropertyOptions(final Class<?> entityClass) {
		final Map<PropertyKey, OptionsContainer> optionsByProperty = new HashMap<PropertyKey, OptionsContainer>();

		for ( final Method method : entityClass.getMethods() ) {
			String propertyName = ReflectionHelper.getPropertyName( method );
			if ( propertyName == null ) {
				continue;
			}

			final OptionsContainer optionsOfProperty = convertOptionAnnotations( method.getAnnotations() );
			optionsByProperty.put( new PropertyKey( entityClass, propertyName ), optionsOfProperty );
		}

		for ( final Field field : entityClass.getDeclaredFields() ) {
			PropertyKey key = new PropertyKey( entityClass, field.getName() );
			OptionsContainer optionsOfField = convertOptionAnnotations( field.getAnnotations() );

			OptionsContainer optionsOfProperty = optionsByProperty.get( key );
			if ( optionsOfProperty != null ) {
				optionsOfProperty.addAll( optionsOfField );
			}
			else {
				optionsByProperty.put( key, optionsOfField );
			}
		}

		return optionsByProperty;
	}

	private static OptionsContainer convertOptionAnnotations(Annotation[] annotations) {
		OptionsContainer options = new OptionsContainer();

		for ( Annotation annotation : annotations ) {
			processAnnotation( options, annotation );
		}

		return options;
	}

	private static <A extends Annotation> void processAnnotation(OptionsContainer options, A annotation) {
		AnnotationConverter<Annotation> converter = getConverter( annotation );

		if ( converter != null ) {
			add( options, converter.convert( annotation ) );
		}
	}

	/**
	 * Returns a converter instance for the given annotation.
	 *
	 * @param annotation the annotation
	 * @return a converter instance or {@code null} if the given annotation is no option annotation
	 */
	private static <A extends Annotation> AnnotationConverter<A> getConverter(Annotation annotation) {
		MappingOption mappingOption = annotation.annotationType().getAnnotation( MappingOption.class );

		// wrong type would be a programming error of the annotation developer
		@SuppressWarnings("unchecked")
		Class<? extends AnnotationConverter<A>> converterClass = (Class<? extends AnnotationConverter<A>>) ( mappingOption != null ? mappingOption.value()
				: null );

		try {
			return converterClass != null ? converterClass.newInstance() : null;
		}
		catch (Exception e) {
			throw log.cannotConvertAnnotation( converterClass, e );
		}
	}

	private static <V> void add(OptionsContainer options, OptionValuePair<V> optionValue) {
		options.add( optionValue.getOption(), optionValue.getValue() );
	}
}
