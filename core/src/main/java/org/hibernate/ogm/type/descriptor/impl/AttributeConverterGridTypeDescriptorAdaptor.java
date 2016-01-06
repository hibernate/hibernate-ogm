/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import javax.persistence.AttributeConverter;
import javax.persistence.PersistenceException;

import org.jboss.logging.Logger;

import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Modeled after {@link org.hibernate.type.descriptor.converter.AttributeConverterSqlTypeDescriptorAdapter}
 * Adapter for incorporating JPA {@link AttributeConverter} handling into the GridTypeDescriptor contract.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class AttributeConverterGridTypeDescriptorAdaptor implements GridTypeDescriptor {
	private static final Logger log = Logger.getLogger( AttributeConverterGridTypeDescriptorAdaptor.class );

	private final AttributeConverter converter;
	private final GridTypeDescriptor delegate;
	private final JavaTypeDescriptor intermediateJavaTypeDescriptor;

	public AttributeConverterGridTypeDescriptorAdaptor(
			AttributeConverter converter,
			GridTypeDescriptor delegate,
			JavaTypeDescriptor intermediateJavaTypeDescriptor) {
		this.converter = converter;
		this.delegate = delegate;
		this.intermediateJavaTypeDescriptor = intermediateJavaTypeDescriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
		// Get the binder for the intermediate type representation
		final GridValueBinder realBinder = delegate.getBinder( intermediateJavaTypeDescriptor );

		return new GridValueBinder<X>() {
			@Override
			public void bind(Tuple resultset, X value, String[] names) {
				final Object convertedValue;
				try {
					convertedValue = converter.convertToDatabaseColumn( value );
				}
				catch (PersistenceException pe) {
					throw pe;
				}
				catch (RuntimeException re) {
					throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
				}

				log.debugf( "Converted value on binding : %s -> %s", value, convertedValue );
				realBinder.bind( resultset, convertedValue, names );
			}
		};
	}

	@Override
	public <X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
		// Get the extractor for the intermediate type representation
		final GridValueExtractor realExtractor = delegate.getExtractor( intermediateJavaTypeDescriptor );

		return new GridValueExtractor<X>() {
			@Override
			public X extract(Tuple resultset, String name) {
				return doConversion( realExtractor.extract( resultset, name ) );
			}

			@SuppressWarnings("unchecked")
			private X doConversion(Object extractedValue) {
				try {
					X convertedValue = (X) converter.convertToEntityAttribute( extractedValue );
					log.debugf( "Converted value on extraction: %s -> %s", extractedValue, convertedValue );
					return convertedValue;
				}
				catch (PersistenceException pe) {
					throw pe;
				}
				catch (RuntimeException re) {
					throw new PersistenceException( "Error attempting to apply AttributeConverter", re );
				}
			}
		};
	}
}
