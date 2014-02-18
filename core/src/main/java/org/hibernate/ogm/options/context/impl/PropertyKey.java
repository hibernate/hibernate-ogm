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
package org.hibernate.ogm.options.context.impl;

/**
 * Represents the lookup key to uniquely identify a property
 */
public class PropertyKey {

	private final Class<?> entity;

	private final String property;

	private final int hashCode;

	public PropertyKey(Class<?> entity, String property ) {
		this.entity = entity;
		this.property = property;
		this.hashCode = computeHashCode( entity, property );
	}

	public Class<?> getEntity() {
		return entity;
	}

	public String getProperty() {
		return property;
	}

	@Override
	public String toString() {
		return entity.getName() + "#" + property;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	private int computeHashCode(Class<?> entity, String property) {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( entity == null ) ? 0 : entity.hashCode() );
		result = prime * result + ( ( property == null ) ? 0 : property.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		PropertyKey other = (PropertyKey) obj;
		if ( entity == null ) {
			if ( other.entity != null ) {
				return false;
			}
		}
		else {
			if ( !entity.equals( other.entity ) ) {
				return false;
			}
		}
		if ( property == null ) {
			if ( other.property != null ) {
				return false;
			}
		}
		else {
			if ( !property.equals( other.property ) ) {
				return false;
			}
		}
		return true;
	}

}
