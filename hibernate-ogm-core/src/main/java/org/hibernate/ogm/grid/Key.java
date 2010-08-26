/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.grid;

import java.io.Serializable;

/**
 * Entity key
 *
 * @author Emmanuel Bernard
 */
public class Key {
	private final String table;
	private final Serializable id;

	public Key(String table, Serializable id) {
		this.table = table;
		this.id = id;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Key" );
		sb.append( "{table=" ).append( table );
		sb.append( ", id=" ).append( id );
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Key key = ( Key ) o;

		if ( id != null ? !id.equals( key.id ) : key.id != null ) {
			return false;
		}
		if ( !table.equals( key.table ) ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = table.hashCode();
		result = 31 * result + ( id != null ? id.hashCode() : 0 );
		return result;
	}
}
