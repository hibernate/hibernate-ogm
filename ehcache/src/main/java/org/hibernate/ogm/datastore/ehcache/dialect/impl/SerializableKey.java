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
package org.hibernate.ogm.datastore.ehcache.dialect.impl;

import java.io.Serializable;
import java.util.Arrays;

import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.Key;
import org.hibernate.ogm.grid.RowKey;

/**
 * Used to serialize {@link Key} objects in Ehcache.
 *
 * @author Gunnar Morling
 */
public class SerializableKey implements Serializable {

	private static final int ENTITY_KEY = 1;
	private static final int ASSOCIATION_KEY = 2;
	private static final int ROW_KEY = 3;

	private final String table;
	private final String[] columnNames;
	private final Object[] columnValues;

	/**
	 * Indicates the specific {@link Key} sub-type.
	 */
	private final int type;

	public SerializableKey(Key key) {
		table = key.getTable();
		columnNames = key.getColumnNames();
		columnValues = key.getColumnValues();

		if ( key instanceof EntityKey ) {
			type = ENTITY_KEY;
		}
		else if ( key instanceof AssociationKey ) {
			type = ASSOCIATION_KEY;
		}
		else if ( key instanceof RowKey ) {
			type = ROW_KEY;
		}
		else {
			throw new IllegalArgumentException( "Unsupported key type: " + key );
		}
	}

	public String getTable() {
		return table;
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	public Object[] getColumnValues() {
		return columnValues;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode( columnNames );
		result = prime * result + Arrays.hashCode( columnValues );
		result = prime * result + ( ( table == null ) ? 0 : table.hashCode() );
		result = prime * result + type;
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
		SerializableKey other = (SerializableKey) obj;
		if ( !Arrays.equals( columnNames, other.columnNames ) ) {
			return false;
		}
		if ( !Arrays.equals( columnValues, other.columnValues ) ) {
			return false;
		}
		if ( table == null ) {
			if ( other.table != null ) {
				return false;
			}
		}
		else if ( !table.equals( other.table ) ) {
			return false;
		}
		if ( type != other.type ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SerializableKey [table=" + table + ", columnNames=" + Arrays.toString( columnNames ) + ", columnValues=" + Arrays.toString( columnValues )
				+ ", type=" + type + "]";
	}
}
