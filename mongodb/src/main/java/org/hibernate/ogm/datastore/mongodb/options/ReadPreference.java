/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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

import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceConverter;
import org.hibernate.ogm.options.spi.MappingOption;

/**
 * Specifies the "read preference" to be applied when performing read operations for the annotated entity or property.
 * <p>
 * When given on the property-level, this setting will only take effect when the property represents an association. If
 * given for non-association properties, the setting on the property-level will be ignored and the setting from the
 * entity will be applied.
 *
 * @author Gunnar Morling
 * @see http://docs.mongodb.org/manual/core/read-preference/
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@MappingOption(ReadPreferenceConverter.class)
public @interface ReadPreference {

	/**
	 * Specifies the read concern setting to be applied when performing read operations for the annotated entity or
	 * property.
	 */
	ReadPreferenceType value();
}
