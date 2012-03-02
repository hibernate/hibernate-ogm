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

/**
 * Entity key
 *
 * @author Emmanuel Bernard
 */
public final class EntityKey implements Serializable {

	private final String table;
	private final Serializable id;
	private final int hashCode;

	public EntityKey(String table, Serializable id) {
		this.table = table;
		this.id = id;
		this.hashCode = generateHashCode();
	}

	public Serializable getId() {
		return id;
	}

	public String getTable() {
		return table;
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
		if ( o == null || EntityKey.class != o.getClass() ) {
			return false;
		}

		EntityKey key = ( EntityKey ) o;

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
		return hashCode;
	}

	private int generateHashCode() {
		final int result = table.hashCode();
		if ( id == null ) {
			return result;
		}
		else {
			return result * 31 + id.hashCode();
		}
	}
}
