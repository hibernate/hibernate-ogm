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
package org.hibernate.ogm.options.navigation.source.impl;

import org.hibernate.ogm.options.container.impl.OptionsContainer;

/**
 * A source for option values. Implementations retrieve option values e.g. using annotations, API invocations or
 * configuration values.
 *
 * @author Gunnar Morling
 */
public interface OptionValueSource {

	/**
	 * Returns an {@link OptionsContainer} with global-level options.
	 *
	 * @return an option container with the global-level options; may be empty but never {@code null}
	 */

	OptionsContainer getGlobalOptions();

	/**
	 * Returns an {@link OptionsContainer} with the entity-level options of the given type.
	 *
	 * @param entityType the type to retrieve the options from
	 * @return an option container with the options of the given type; may be empty but never {@code null}
	 */
	OptionsContainer getEntityOptions(Class<?> entityType);

	/**
	 * Returns an {@link OptionsContainer} with the property-level options of the given property.
	 *
	 * @param entityType type declaring the property to retrieve the options from
	 * @param propertyName name of the property to retrieve the options from
	 * @return an option container with options of the given property; may be empty but never {@code null}
	 */
	OptionsContainer getPropertyOptions(Class<?> entityType, String propertyName);
}
