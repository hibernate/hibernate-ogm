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

/**
 * Entity key
 *
 * @author Emmanuel Bernard
 */
public final class EntityKey implements Serializable, Key {

	private final EntityKeyMetadata keyMetadata;
	private final int hashCode;
	private final Object[] columnValues;

	public EntityKey(EntityKeyMetadata keyMetadata, Object[] values) {
		this.keyMetadata = keyMetadata;
		this.columnValues = values;
		this.hashCode = generateHashCode();
	}

	@Override
	public String getTable() {
		return keyMetadata.getTable();
	}

	/**
	 * This class should be treated as immutable. While we expose this array,
	 * you should never make changes to it!
	 * This is a design tradeoff vs. raw performance and memory usage.
	 */
	@Override
	public Object[] getColumnValues() {
		return columnValues;
	}

	/**
	 * This class should be treated as immutable. While we expose this array,
	 * you should never make changes to it!
	 * This is a design tradeoff vs. raw performance and memory usage.
	 */
	@Override
	public String[] getColumnNames() {
		return keyMetadata.getColumnNames();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "EntityKey" );
		sb.append( "{table='" ).append( keyMetadata.getTable() ).append( '\'' );
		String[] columnNames = keyMetadata.getColumnNames();
		sb.append( ", columnNames=" ).append( columnNames == null ? "null" : Arrays.asList( columnNames ).toString() );
		sb.append( ", columnValues=" ).append( columnValues == null ? "null" : Arrays.asList( columnValues ).toString() );
		sb.append( '}' );
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
