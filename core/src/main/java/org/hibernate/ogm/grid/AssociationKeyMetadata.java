/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	private String[] rowKeyColumnNames;

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

		AssociationKeyMetadata that = (AssociationKeyMetadata) o;

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
