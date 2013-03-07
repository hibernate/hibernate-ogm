/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2013 Red Hat Inc. and/or its affiliates and other contributors
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
 */
public final class AssociationKey implements Serializable {

	//column value types do have to be serializable so AssociationKey is serializable
	//should it be a Serializable[] type? It seems to be more pain than anything else
	private final AssociationKeyMetadata metadata;
	private final Object[] columnValues;
	private final int hashCode;

	//role and entity key are not part of the object identity
	private transient String collectionRole;
	private transient EntityKey entityKey;
	private transient AssociationKind associationKind;

	public AssociationKey(AssociationKeyMetadata metadata, Object[] columnValues) {
		this.metadata = metadata;
		if ( metadata.getColumnNames().length != columnValues.length ) {
			throw new AssertionFailure( "Column names do not match column values" );
		}
		this.columnValues = columnValues;
		this.hashCode = metadata.hashCode() * 31 + Arrays.hashCode( columnValues );
	}

	public String getTable() {
		return metadata.getTable();
	}

	public String[] getColumnNames() {
		return metadata.getColumnNames();
	}

	public Object[] getColumnValues() {
		return columnValues;
	}

	/**
	 * Association role. May be null but is typically filled for collection of embeddable.
	 */
	public String getCollectionRole() {
		return collectionRole;
	}

	/**
	 * Owning entity key. May be null but is typically filled for collection of embeddable.
	 */
	public EntityKey getEntityKey() {
		return entityKey;
	}

	/**
	 * Describe the type of association. May be null but is typically filled for collection of embeddable.
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

		AssociationKey that = ( AssociationKey ) o;

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

	public void setCollectionRole(String role) {
		this.collectionRole = role;
	}

	public void setOwnerEntityKey(EntityKey entityKey) {
		this.entityKey = entityKey;
	}

	public void setAssociationKind(AssociationKind kind) {
		this.associationKind = kind;
	}
}
