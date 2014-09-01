/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.grid;

import java.util.Arrays;

import org.hibernate.annotations.common.AssertionFailure;

/**
 * Represents the key used to link a property value and the id of it's owning entity
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Gunnar Morling
 */
public final class AssociationKey implements Key {

	//column value types do have to be serializable so AssociationKey is serializable
	//should it be a Serializable[] type? It seems to be more pain than anything else
	private final AssociationKeyMetadata metadata;
	private final Object[] columnValues;
	private final int hashCode;

	//role and entity key are not part of the object identity
	private final String collectionRole;
	private final EntityKey entityKey;
	private final AssociationKind associationKind;

	public AssociationKey(AssociationKeyMetadata metadata, Object[] columnValues, String collectionRole, EntityKey entityKey, AssociationKind associationKind) {
		this.metadata = metadata;
		if ( metadata.getColumnNames().length != columnValues.length ) {
			throw new AssertionFailure( "Column names do not match column values" );
		}
		this.columnValues = columnValues;
		this.collectionRole = collectionRole;
		this.entityKey = entityKey;
		this.associationKind = associationKind;

		this.hashCode = metadata.hashCode() * 31 + Arrays.hashCode( columnValues );
	}

	public AssociationKeyMetadata getMetadata() {
		return metadata;
	}

	/**
	 * Returns the table name of this key.
	 */
	public String getTable() {
		return metadata.getTable();
	}

	/**
	 * The columns identifying the association.
	 *
	 * For example, in a many to many association, the row key will look like:
	 *
	 * <pre>
	 * RowKey{table='AccountOwner_BankAccount', columnNames=[owners_id, bankAccounts_id], columnValues=[...]},
	 * </pre>
	 *
	 * the association key will be something like:
	 *
	 * <pre>
	 * AssociationKey{table='AccountOwner_BankAccount', columnNames=[owners_id], columnValues=[...]},
	 * </pre>
	 */
	@Override
	public String[] getColumnNames() {
		return metadata.getColumnNames();
	}

	@Override
	public Object[] getColumnValues() {
		return columnValues;
	}

	/**
	 * Returns the association role.
	 */
	public String getCollectionRole() {
		return collectionRole;
	}

	/**
	 * Returns the owning entity key.
	 */
	public EntityKey getEntityKey() {
		return entityKey;
	}

	/**
	 * Returns the type of association
	 */
	public AssociationKind getAssociationKind() {
		return associationKind;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || AssociationKey.class != o.getClass() ) {
			return false;
		}

		AssociationKey that = (AssociationKey) o;

		// order of comparison matters on performance:
		if ( !metadata.getTable().equals( that.metadata.getTable() ) ) {
			return false;
		}

		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if ( !Arrays.equals( columnValues, that.columnValues ) ) {
			return false;
		}
		if ( !Arrays.equals( metadata.getColumnNames(), that.metadata.getColumnNames() ) ) {
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
		sb.append( "AssociationKey" );
		sb.append( "{table='" ).append( metadata.getTable() ).append( '\'' );
		String[] columnNames = metadata.getColumnNames();
		sb.append( ", columnNames=" ).append( columnNames == null ? "null" : Arrays.asList( columnNames ).toString() );
		sb.append( ", columnValues=" ).append( columnValues == null ? "null" : Arrays.asList( columnValues ).toString() );
		sb.append( '}' );
		return sb.toString();
	}

	/**
	 * Returns the value of the given column if part of this key. Use {@link AssociationKeyMetadata#isKeyColumn(String)}
	 * to check whether a given column is part of this key prior to invoking this method.
	 *
	 * @param columnName the name of interest
	 * @return the value of the given column.
	 */
	public Object getColumnValue(String columnName) {
		for ( int i = 0; i < getColumnNames().length; i++ ) {
			String name = getColumnNames()[i];
			if ( name.equals( columnName ) ) {
				return getColumnValues()[i];
			}
		}

		throw new AssertionFailure(
				String.format( "Given column %s is not part of this key: %s", columnName, this.toString() )
		);
	}
}
