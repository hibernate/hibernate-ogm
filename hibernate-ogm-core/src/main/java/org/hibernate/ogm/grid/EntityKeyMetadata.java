package org.hibernate.ogm.grid;

import java.util.Arrays;

/**
 * Stores metadata information common to all keys related
 * to a given entity.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class EntityKeyMetadata {
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
		if ( this == o ) return true;
		if ( o == null || EntityKeyMetadata.class != o.getClass() ) return false;

		EntityKeyMetadata entityKeyMetadata = (EntityKeyMetadata) o;

		//table is easier to compare first
		if ( !table.equals( entityKeyMetadata.table ) ) return false;
		if ( !Arrays.equals( columnNames, entityKeyMetadata.columnNames ) ) return false;

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

