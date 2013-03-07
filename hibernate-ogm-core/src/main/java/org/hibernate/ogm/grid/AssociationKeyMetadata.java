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

import java.util.Arrays;

/**
 * Stores metadata information common to all keys related
  * to a given association
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class AssociationKeyMetadata {
	private final String table;
	private final String[] columnNames;
	private final int hashCode;

	//role and entity key are not part of the object identity
	private transient String[] rowKeyColumnNames;

	public AssociationKeyMetadata(String table, String[] columnNames) {
		this.table = table;
		this.columnNames = columnNames;
		// table hashing should be specific enough
		this.hashCode = table.hashCode();
	}

	public String getTable() {
		return table;
	}

	public String[] getColumnNames() {
		return columnNames;
	}

	public String[] getRowKeyColumnNames() {
		return rowKeyColumnNames;
	}

	public void setRowKeyColumnNames(String[] rowKeyColumnNames) {
		this.rowKeyColumnNames = rowKeyColumnNames;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || AssociationKeyMetadata.class != o.getClass() ) {
			return false;
		}

		AssociationKeyMetadata that = ( AssociationKeyMetadata ) o;

		// order of comparison matters on performance:
		if ( !table.equals( that.table ) ) {
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

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AssociationKeyMetadata" );
		sb.append( "{table='" ).append( table ).append( '\'' );
		sb.append( ", columnNames=" ).append( columnNames == null ? "null" : Arrays.asList( columnNames ).toString() );
		sb.append( '}' );
		return sb.toString();
	}
}
