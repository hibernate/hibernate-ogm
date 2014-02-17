/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.grid;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents the key used to represent a row
 *
 * @author Emmanuel Bernard
 */
public final class RowKey implements Serializable, Key {

	private final String table;
	private final String[] columnNames;
	//column value types do have to be serializable so RowKey can be serializable
	//should it be a Serializable[] type? It seems to be more pain than anything else
	private final Object[] columnValues;
	private final int hashCode;

	public RowKey(String table, String[] columnNames, Object[] columnValues) {
		this.table = table;
		this.columnNames = columnNames;
		this.columnValues = columnValues;
		this.hashCode = generateHashCode();
	}

	@Override
	public String getTable() {
		return table;
	}

	/**
	 * This class should be treated as immutable. While we expose this array,
	 * you should never make changes to it!
	 * This is a design tradeoff vs. raw performance and memory usage.
	 */
	@Override
	public String[] getColumnNames() {
		return columnNames;
	}

	/**
	 * This class should be treated as immutable. While we expose this array,
	 * you should never make changes to it!
	 * This is a design tradeoff vs. raw performance and memory usage.
	 */
	@Override
	public Object[] getColumnValues() {
		return columnValues;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || RowKey.class != o.getClass() ) {
			return false;
		}

		RowKey that = (RowKey) o;

		if ( !table.equals( that.table ) ) {
			return false;
		}

		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if ( !Arrays.equals( columnValues, that.columnValues ) ) {
			return false;
		}
		if ( !Arrays.equals( columnNames, that.columnNames ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	private int generateHashCode() {
		final int result = table.hashCode();
		return 31 * result + Arrays.hashCode( columnValues );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "RowKey" );
		sb.append( "{table='" ).append( table ).append( '\'' );
		sb.append( ", columnNames=" ).append( columnNames == null ? "null" : Arrays.asList( columnNames ).toString() );
		sb.append( ", columnValues=" )
				.append( columnValues == null ? "null" : Arrays.asList( columnValues ).toString() );
		sb.append( '}' );
		return sb.toString();
	}
}
