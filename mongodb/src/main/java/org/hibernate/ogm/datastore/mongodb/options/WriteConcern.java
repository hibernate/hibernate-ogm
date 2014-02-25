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

import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernConverter;
import org.hibernate.ogm.options.spi.MappingOption;

/**
 * Specifies the write concern to be applied when performing write operations to the annotated entity or property. Can
 * either be given using a pre-configured write concern such as {@link WriteConcernType#JOURNALED} or by specifying the
 * type of a custom {@link WriteConcern} implementation.
 * <p>
 * When given on the property-level, this setting will only take affect when the property represents an association and
 * this association is stored as a separate association document. If given for non-association properties or embedded
 * associations, the setting on the property-level will be ignored and the setting from the entity will be applied.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@MappingOption(WriteConcernConverter.class)
public @interface WriteConcern {

	/**
	 * Specifies the write concern to be applied when performing write operations to the annotated entity or property.
	 * <p>
	 * Use {@link WriteConcernType#CUSTOM} in conjunction with {@link #type()} to specify a custom {@link WriteConcern}
	 * implementation. This is useful in cases where the pre-defined configurations are not sufficient, e.g. if you want
	 * to ensure that writes are propagated to a specific number of replicas or given "tag set".
	 */
	WriteConcernType value();

	/**
	 * Specifies a custom {@link com.mongodb.WriteConcern} implementation. Only takes effect if {@link #value()} is set
	 * to {@link WriteConcernType#CUSTOM}. The specified type must have a default (no-args) constructor.
	 */
	Class<? extends com.mongodb.WriteConcern> type() default com.mongodb.WriteConcern.class;
}
