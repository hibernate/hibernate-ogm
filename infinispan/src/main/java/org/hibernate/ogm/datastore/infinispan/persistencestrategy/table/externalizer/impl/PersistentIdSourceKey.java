/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl;

import org.hibernate.ogm.model.key.spi.IdSourceKey;

/**
 * Identifies an id source key stored in Infinispan when using the "per table" strategy.
 *
 * @author Gunnar Morling
 */
public class PersistentIdSourceKey {

	private final String name;
	private final Object value;

	public PersistentIdSourceKey(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	public static PersistentIdSourceKey fromIdSourceKey(IdSourceKey key) {
		return new PersistentIdSourceKey( key.getColumnNames()[0], key.getColumnValues()[0] );
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		result = prime * result + ( ( value == null ) ? 0 : value.hashCode() );
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
		PersistentIdSourceKey other = (PersistentIdSourceKey) obj;
		if ( name == null ) {
			if ( other.name != null ) {
				return false;
			}
		}
		else if ( !name.equals( other.name ) ) {
			return false;
		}
		if ( value == null ) {
			if ( other.value != null ) {
				return false;
			}
		}
		else if ( !value.equals( other.value ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "PersistentIdSourceKey [name=" + name + ", value=" + value + "]";
	}
}
