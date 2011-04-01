/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
 */
public class AssociationKey implements Serializable {
	private final String table;
	private final String[] columnNames;
	//column value types do have to be serializable so AssociationKey is serializable
	//should it be a Serializable[] type? It seems to be more pain than anything else
	private final Object[] columnValues;

	public AssociationKey(String table, String[] columnNames, Object[] columnValues) {
		if ( columnNames.length != columnValues.length ) {
			throw new AssertionFailure( "Column names do not match column values" );
		}
		this.table = table;
		this.columnNames = columnNames;
		this.columnValues = columnValues;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		AssociationKey that = ( AssociationKey ) o;

		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if ( !Arrays.equals( columnValues, that.columnValues ) ) {
			return false;
		}
		if ( !Arrays.equals( columnNames, that.columnNames ) ) {
			return false;
		}
		if ( !table.equals( that.table ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = table.hashCode();
		result = 31 * result + Arrays.hashCode( columnNames );
		result = 31 * result + Arrays.hashCode( columnValues );
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "AssociationKey" );
		sb.append( "{table='" ).append( table ).append( "'" );
		for( int index = 0 ; index < columnNames.length ; index++) {
			sb.append("\n\t").append( columnNames[index] ).append( " = '" ).append( columnValues[index] ).append( "'" );
		}

		sb.append( "\n}" );
		return sb.toString();
	}
}
