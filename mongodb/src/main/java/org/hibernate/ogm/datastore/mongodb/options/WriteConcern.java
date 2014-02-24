/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb.options;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.ogm.datastore.mongodb.options.WriteConcern.WriteConcernConverter;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.ogm.options.spi.MappingOption;
import org.hibernate.ogm.options.spi.OptionValuePair;

/**
 * Specifies the write concern to be applied when performing write operations to the annotated entity or property.
 * <p>
 * When given on the property-level, this setting will only take affect when the property represents an association and
 * this association is stored as a separate association document. If given for non-association properties or embedded
 * associations, the setting on the property-level will be ignored and the setting from the entiy will be applied.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@MappingOption(WriteConcernConverter.class)
public @interface WriteConcern {

	/**
	 * Specifies the write concern to be applied when performing write operations to the annotated entity or property.
	 */
	WriteConcernType value();

	static class WriteConcernConverter implements AnnotationConverter<WriteConcern> {

		@Override
		public OptionValuePair<?> convert(WriteConcern annotation) {
			return OptionValuePair.getInstance( new WriteConcernOption(), annotation.value() );
		}
	}
}
