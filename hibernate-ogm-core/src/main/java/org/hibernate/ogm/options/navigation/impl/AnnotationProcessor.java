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

import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.MappingOption;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class AnnotationProcessor {

	private static final Log log = LoggerFactory.make();

	public static void saveEntityOptions(final MappingContext context, final Class<?> entityClass) {
		context.configureEntity( entityClass );
		Annotation[] annotations = entityClass.getAnnotations();
		saveOptions( new ContextCommand() {

			@Override
			public void add(Option<?, ?> option) {
				context.addEntityOption( option );
			}

		}, annotations );
	}

	public static void savePropertyOptions(final MappingContext context, final Class<?> entityClass) {
		context.configureEntity( entityClass );
		for ( final Method method : entityClass.getMethods() ) {
			context.configureProperty( method.getName() );
			saveOptions( new ContextCommand() {

				@Override
				public void add(Option<?, ?> option) {
					context.addPropertyOption( option );
				}

			}, method.getAnnotations() );
		}
		for ( final Field field : entityClass.getFields() ) {
			context.configureProperty( field.getName() );
			saveOptions( new ContextCommand() {

				@Override
				public void add(Option<?, ?> option) {
					context.addPropertyOption( option );
				}

			}, field.getAnnotations() );
		}
	}

	private static void saveOptions(ContextCommand command, Annotation[] annotations) {
		for ( Annotation annotation : annotations ) {
			Class<? extends Annotation> class1 = annotation.annotationType();
			Annotation[] qualifiers = class1.getAnnotations();
			for ( Annotation qualifier : qualifiers ) {
				if ( qualifier.annotationType().equals( MappingOption.class ) ) {
					Class<? extends AnnotationConverter<?>> converterClass = ( (MappingOption) qualifier ).value();
					Option<?, ?> option = convert( annotation, converterClass );
					command.add( option );
					break;
				}
			}
		}
	}

	private static Option<?, ?> convert(Annotation annotation, Class<? extends AnnotationConverter<?>> converterClass) {
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
		void add(Option<?, ?> option);
	}

}
