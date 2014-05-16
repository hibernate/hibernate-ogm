/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.impl;

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
