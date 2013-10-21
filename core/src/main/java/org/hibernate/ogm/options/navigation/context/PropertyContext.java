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
package org.hibernate.ogm.options.navigation.context;

import java.lang.annotation.ElementType;

import org.hibernate.ogm.options.spi.PropertyOptions;

/**
 * Property level to the mapping API. Implementations must declare a constructor with a single parameter of type
 * {@link org.hibernate.ogm.options.navigation.impl.ConfigurationContext} and should preferably be derived from
 * {@link org.hibernate.ogm.options.navigation.impl.BasePropertyContext}.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @param <E> the type of provider-specific entity context definition, associated with the specific property context
 * type
 * @param <P> the type of a provider-specific property context definition, following the self-referential generic type
 * pattern
 */
public interface PropertyContext<E extends EntityContext<E, P>, P extends PropertyContext<E, P>> extends PropertyOptions<P> {

	/**
	 * Specify mapping for the entity {@code type}
	 */
	E entity(Class<?> type);

	/**
	 * Specify mapping for the given property.
	 *
	 * @param propertyName the name of the property to be configured, following to the JavaBeans naming convention
	 * @param target the target element type of the property, must either be {@link ElementType#FIELD} or
	 * {@link ElementType#METHOD}).
	 */
	P property(String propertyName, ElementType target);
}
