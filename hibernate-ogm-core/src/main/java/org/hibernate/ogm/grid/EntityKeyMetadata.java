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
package org.hibernate.ogm.grid;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Stores metadata information common to all keys related
 * to a given entity.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class EntityKeyMetadata implements Serializable {
	private final String table;
	private final int hashCode;
	private String[] columnNames;

	public EntityKeyMetadata(String tableName, String[] columnNames) {
		this.table = tableName;
		this.columnNames = columnNames;
		this.hashCode = generateHashCode();
	}

	public String getTable() {
		return table;
	}

	/**
	 * This class should be treated as immutable. While we expose this array,
	 * you should never make changes to it!
	 * This is a design tradeoff vs. raw performance and memory usage.
	 */
	public String[] getColumnNames() {
		return columnNames;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntityKeyMetadata" );
		sb.append( "{table='" ).append( table ).append( '\'' );
		sb.append( ", columnNames=" ).append( columnNames == null ? "null" : Arrays.asList( columnNames ).toString() );
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || EntityKeyMetadata.class != o.getClass() ) {
			return false;
		}

		EntityKeyMetadata entityKeyMetadata = (EntityKeyMetadata) o;

		//table is easier to compare first
		if ( !table.equals( entityKeyMetadata.table ) ) {
			return false;
		}
		if ( !Arrays.equals( columnNames, entityKeyMetadata.columnNames ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	private int generateHashCode() {
		//Note we don't hash on the column names as the hash will discriminate enough
		//with table and Arrays.hashCode is not cheap
		int result = table.hashCode();
		return result;
	}
}

