/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2014 Red Hat Inc. and/or its affiliates and other contributors
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

import org.hibernate.annotations.common.AssertionFailure;

/**
 * Represents the key used to link a property value and the id of it's owning entity
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @author Gunnar Morling
 */
public final class AssociationKey implements Serializable, Key {

	//column value types do have to be serializable so AssociationKey is serializable
	//should it be a Serializable[] type? It seems to be more pain than anything else
	private final AssociationKeyMetadata metadata;
	private final Object[] columnValues;
	private final int hashCode;

	//role and entity key are not part of the object identity
	private transient String collectionRole;
	private transient EntityKey entityKey;
	private transient AssociationKind associationKind;

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

	@Override
	public String getTable() {
		return metadata.getTable();
	}

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

	public String[] getRowKeyColumnNames() {
		return metadata.getRowKeyColumnNames();
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
	 * Whether the given column is part of this key or not.
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

	/**
	 * Returns the value of the given column if part of this key. Use {@link AssociationKey#isKeyColumn(String)} to
	 * check whether a given column is part of this key prior to invoking this method.
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
