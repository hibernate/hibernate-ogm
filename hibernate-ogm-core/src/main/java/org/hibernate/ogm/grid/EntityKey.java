/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
 * Entity key
 *
 * @author Emmanuel Bernard
 */
public final class EntityKey implements Serializable {

	private final String table;
	private final int hashCode;
	private String[] columnNames;
	private Object[] columnValues;

	public EntityKey(String tableName, String[] columnNames, Object[] values) {
		this.table = tableName;
		this.columnNames = columnNames;
		this.columnValues = values;
		this.hashCode = generateHashCode();
	}

	public String getTable() {
		return table;
	}

	public Object[] getColumnValues() {
		return columnValues;
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntityKey" );
		sb.append( "{table='" ).append( table ).append( '\'' );
		sb.append( ", columnNames=" ).append( columnNames == null ? "null" : Arrays.asList( columnNames ).toString() );
		sb.append( ", columnValues=" ).append( columnValues == null ? "null" : Arrays.asList( columnValues ).toString() );
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( o == null || EntityKey.class != o.getClass() ) return false;

		EntityKey entityKey = (EntityKey) o;

		//values are more discriminatory, test first
		if ( !Arrays.equals( columnValues, entityKey.columnValues ) ) return false;
		if ( !Arrays.equals( columnNames, entityKey.columnNames ) ) return false;
		if ( !table.equals( entityKey.table ) ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	private int generateHashCode() {
		//Note we don't hash on the column names as the hash will discriminate enough
		//with values and Arrays.hashCode is nto cheap
		int result = table.hashCode();
		result = 31 * result + Arrays.hashCode( columnValues );
		return result;
	}
}
