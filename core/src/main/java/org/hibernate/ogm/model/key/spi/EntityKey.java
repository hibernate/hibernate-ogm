/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.key.spi;

import java.util.Arrays;

/**
 * Represents the key of an entity.
 *
 * @author Emmanuel Bernard
 */
public final class EntityKey {

	private final EntityKeyMetadata keyMetadata;
	private final int hashCode;
	private final Object[] columnValues;

	public EntityKey(EntityKeyMetadata keyMetadata, Object[] values) {
		this.keyMetadata = keyMetadata;
		this.columnValues = values;
		this.hashCode = generateHashCode();
	}

	/**
	 * Returns the table name of this key.
	 */
	public String getTable() {
		return keyMetadata.getTable();
	}

	/**
	 * This class should be treated as immutable. While we expose this array,
	 * you should never make changes to it!
	 * This is a design tradeoff vs. raw performance and memory usage.
	 */
	public Object[] getColumnValues() {
		return columnValues;
	}

	/**
	 * This class should be treated as immutable. While we expose this array,
	 * you should never make changes to it!
	 * This is a design tradeoff vs. raw performance and memory usage.
	 */
	public String[] getColumnNames() {
		return keyMetadata.getColumnNames();
	}

	public EntityKeyMetadata getMetadata() {
		return keyMetadata;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntityKey(" );
		sb.append( getTable() );
		sb.append( ") [" );
		int i = 0;
		for ( String column : keyMetadata.getColumnNames() ) {
			sb.append( column ).append( "=" ).append( columnValues[i] );
			i++;
			if ( i < keyMetadata.getColumnNames().length ) {
				sb.append( ", " );
			}
		}
		sb.append( "]" );
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || EntityKey.class != o.getClass() ) {
			return false;
		}

		EntityKey entityKey = (EntityKey) o;

		//values are more discriminatory, test first
		if ( !Arrays.equals( columnValues, entityKey.columnValues ) ) {
			return false;
		}
		if ( !keyMetadata.equals( entityKey.keyMetadata )) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	private int generateHashCode() {
		int result = keyMetadata.hashCode();
		result = 31 * result + Arrays.hashCode( columnValues );
		return result;
	}
}
