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
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Reads the annotation on an entity and save them in the appropriate context as {@link Option}.
 * <p>
 * Only the annotations representing an option are considered.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class AnnotationProcessor {

	private static final Log log = LoggerFactory.make();

	/**
	 * Saves in the {@link OptionsContext} the options that are related to an entity.
	 *
	 * @param context the {@link OptionsContext} where {@link Option} are saved
	 * @param entityClass class of the entity annotated with the options
	 */
	public static OptionsContainer getEntityOptions(Class<?> entityType) {
		Annotation[] annotations = entityType.getAnnotations();
		final OptionsContainer container = new OptionsContainer();

		saveOptions( new ContextCommand() {

			@Override
			public void add(Option<?> option) {
				container.add( option );
			}

		}, annotations );

		return container;
	}

	/**
	 * Saves in the {@link OptionsContext} the options that are related to the properties and methods of an entity.
	 *
	 * @param context the {@link OptionsContext} where {@link Option} are saved
	 * @param entityClass class containing the option annotation
	 */
	public static Map<PropertyKey, OptionsContainer> getPropertyOptions(final Class<?> entityClass) {
		final Map<PropertyKey, OptionsContainer> optionsByProperty = new HashMap<PropertyKey, OptionsContainer>();

		for ( final Method method : entityClass.getMethods() ) {
			final OptionsContainer optionsOfProperty = new OptionsContainer();
			//TODO OGM-345 Use property name
			optionsByProperty.put( new PropertyKey( entityClass, method.getName() ), optionsOfProperty );

			saveOptions( new ContextCommand() {

				@Override
				public void add(Option<?> option) {
					optionsOfProperty.add( option );
				}

			}, method.getAnnotations() );
		}

		for ( final Field field : entityClass.getFields() ) {
			PropertyKey key = new PropertyKey( entityClass, field.getName() );
			OptionsContainer optionsOfProperty = optionsByProperty.get( key );
			if ( optionsOfProperty == null ) {
				optionsOfProperty = new OptionsContainer();
				optionsByProperty.put( new PropertyKey( entityClass, field.getName() ), optionsOfProperty );
			}

			final OptionsContainer options = optionsOfProperty;

			saveOptions( new ContextCommand() {

				@Override
				public void add(Option<?> option) {
					options.add( option );
				}

			}, field.getAnnotations() );
		}

		return optionsByProperty;
	}

	private static void saveOptions(ContextCommand command, Annotation[] annotations) {
		for ( Annotation annotation : annotations ) {
			Class<? extends Annotation> class1 = annotation.annotationType();
			Annotation[] qualifiers = class1.getAnnotations();
			for ( Annotation qualifier : qualifiers ) {
				if ( qualifier.annotationType().equals( MappingOption.class ) ) {
					Class<? extends AnnotationConverter<?>> converterClass = ( (MappingOption) qualifier ).value();
					Option<?> option = convert( annotation, converterClass );
					command.add( option );
					break;
				}
			}
		}
	}

	private static Option<?> convert(Annotation annotation, Class<? extends AnnotationConverter<?>> converterClass) {
		try {
			AnnotationConverter converter = converterClass.newInstance();
			return converter.convert( annotation );
		}
		catch (InstantiationException e) {
			throw log.cannotConvertAnnotation( converterClass, e );
		}
		catch (IllegalAccessException e) {
			throw log.cannotConvertAnnotation( converterClass, e );
		}
	}

	private interface ContextCommand {
		void add(Option<?> option);
	}

}
