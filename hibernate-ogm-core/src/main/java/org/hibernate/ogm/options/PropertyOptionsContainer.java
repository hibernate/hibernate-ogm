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
package org.hibernate.ogm.options;

import org.hibernate.ogm.mapping.impl.MappingContext.PropertyKey;
import org.hibernate.ogm.mapping.impl.OptionsContainer;

/**
 * A container for options that can be applied to a property.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class PropertyOptionsContainer extends EntityOptionsContainer implements OptionsContainer {

	private final PropertyKey propertyKey;

	/**
	 * Create a new container for options that can be applied to a property.
	 *
	 * @param propertyKey
	 *            identifying the property
	 */
	public PropertyOptionsContainer(PropertyKey propertyKey) {
		super( propertyKey.getEntity() );
		this.propertyKey = propertyKey;
	}

	public PropertyKey getPropertyKey() {
		return propertyKey;
	}

}
