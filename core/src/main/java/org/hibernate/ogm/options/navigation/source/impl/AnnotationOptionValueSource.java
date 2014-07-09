/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.source.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.container.impl.OptionsContainerBuilder;
import org.hibernate.ogm.options.navigation.impl.PropertyKey;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.MappingOption;
import org.hibernate.ogm.options.spi.OptionValuePair;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.ReflectionHelper;

/**
 * An {@link OptionValueSource} which retrieves option values from Java annotations.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class AnnotationOptionValueSource implements OptionValueSource {

	private static final Log log = LoggerFactory.make();

	@Override
	public OptionsContainer getGlobalOptions() {
		return OptionsContainer.EMPTY;
	}

	@Override
	public OptionsContainer getEntityOptions(Class<?> entityType) {
		OptionsContainerBuilder options = convertOptionAnnotations( entityType.getAnnotations() );
		return options != null ? options.build() : OptionsContainer.EMPTY;
	}

	@Override
	public OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName) {
		OptionsContainerBuilder options = getPropertyOptions( entityType ).get( new PropertyKey( entityType, propertyName ) );
		return options != null ? options.build() : OptionsContainer.EMPTY;
	}

	private Map<PropertyKey, OptionsContainerBuilder> getPropertyOptions(final Class<?> entityClass) {
		final Map<PropertyKey, OptionsContainerBuilder> optionsByProperty = new HashMap<PropertyKey, OptionsContainerBuilder>();

		for ( final Method method : entityClass.getMethods() ) {
			String propertyName = ReflectionHelper.getPropertyName( method );
			if ( propertyName == null ) {
				continue;
			}

			final OptionsContainerBuilder optionsOfProperty = convertOptionAnnotations( method.getAnnotations() );
			if ( optionsOfProperty != null ) {
				optionsByProperty.put( new PropertyKey( entityClass, propertyName ), optionsOfProperty );
			}
		}

		for ( final Field field : entityClass.getDeclaredFields() ) {
			PropertyKey key = new PropertyKey( entityClass, field.getName() );
			OptionsContainerBuilder optionsOfField = convertOptionAnnotations( field.getAnnotations() );

			if ( optionsOfField != null ) {
				OptionsContainerBuilder optionsOfProperty = optionsByProperty.get( key );
				if ( optionsOfProperty != null ) {
					optionsOfProperty.addAll( optionsOfField );
				}
				else {
					optionsByProperty.put( key, optionsOfField );
				}
			}
		}

		return optionsByProperty;
	}

	private OptionsContainerBuilder convertOptionAnnotations(Annotation[] annotations) {
		OptionsContainerBuilder builder = null;

		for ( Annotation annotation : annotations ) {
			builder = processAnnotation( builder, annotation );
		}

		return builder;
	}

	private <A extends Annotation> OptionsContainerBuilder processAnnotation(OptionsContainerBuilder builder, A annotation) {
		AnnotationConverter<Annotation> converter = getConverter( annotation );

		if ( converter != null ) {
			if ( builder == null ) {
				builder = new OptionsContainerBuilder();
			}
			add( builder, converter.convert( annotation ) );
		}

		return builder;
	}

	/**
	 * Returns a converter instance for the given annotation.
	 *
	 * @param annotation the annotation
	 * @return a converter instance or {@code null} if the given annotation is no option annotation
	 */
	private <A extends Annotation> AnnotationConverter<A> getConverter(Annotation annotation) {
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

	private <V> void add(OptionsContainerBuilder builder, OptionValuePair<V> optionValue) {
		builder.add( optionValue.getOption(), optionValue.getValue() );
	}
}
