/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.spi;

/**
 * Operation applied to the Tuple.
 * A column name is provided and when it makes sense a column value
 * (eg DELETE or PUT_NULL do not have column value)
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class TupleOperation {
	private final String column;
	private final Object value;
	private final TupleOperationType type;

	public TupleOperation(String column, Object value, TupleOperationType type) {
		this.column = column;
		this.value = value;
		this.type = type;
	}

	public String getColumn() {
		return column;
	}

	public Object getValue() {
		return value;
	}

	public TupleOperationType getType() {
		return type;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "TupleOperation" );
		sb.append( "{type=\'" ).append( type ).append( '\'' );
		sb.append( ", column='" ).append( column ).append( '\'' );
		sb.append( ", value=\'" ).append( value ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}
