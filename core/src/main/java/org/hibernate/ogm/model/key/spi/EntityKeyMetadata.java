/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.key.spi;

import java.util.Arrays;

/**
 * Stores metadata information common to all keys related to a given entity.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class EntityKeyMetadata {

	private final String table;
	private final int hashCode;
	private final String[] columnNames;

	public EntityKeyMetadata(String tableName, String[] columnNames) {
		this.table = tableName;
		this.columnNames = columnNames;
		this.hashCode = generateHashCode();
	}

	public String getTable() {
		return table;
	}

	/**
	 * This class should be treated as immutable. While we expose this array, you should never make changes to it! This
	 * is a design tradeoff vs. raw performance and memory usage.
	 */
	public String[] getColumnNames() {
		return columnNames;
	}

	/**
	 * Whether the given column is part of this key family or not.
	 *
	 * @return {@code true} if the given column is part of this key, {@code false} otherwise.
	 */
	public boolean isKeyColumn(String columnName) {
		for ( String keyColumName : getColumnNames() ) {
			if ( keyColumName.equals( columnName ) ) {
				return true;
			}
		}

		return false;
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

		// table is easier to compare first
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
		// Note we don't hash on the column names as the hash will discriminate enough
		// with table and Arrays.hashCode is not cheap
		int result = table.hashCode();
		return result;
	}
}
