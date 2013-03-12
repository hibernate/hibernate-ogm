/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 *  JBoss, Home of Professional Open Source
 *  Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 *  as indicated by the @authors tag. All rights reserved.
 *  See the copyright.txt in the distribution for a
 *  full listing of individual contributors.
 *
 *  This copyrighted material is made available to anyone wishing to use,
 *  modify, copy, or redistribute it subject to the terms and conditions
 *  of the GNU Lesser General Public License, v. 2.1.
 *  This program is distributed in the hope that it will be useful, but WITHOUT A
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *  You should have received a copy of the GNU Lesser General Public License,
 *  v.2.1 along with this distribution; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA  02110-1301, USA.
 */

package org.hibernate.ogm.dialect.cassandra.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.datastax.driver.core.Row;

import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class CassandraAssociationSnapshot implements AssociationSnapshot {
	private final List<Row> cqlRows;
	private final AssociationKey associationKey;
	private final RowKey rowKey;

	public CassandraAssociationSnapshot(RowKey rowKey, List<Row> cqlRows, AssociationKey associationKey) {
		this.associationKey = associationKey;
		this.cqlRows = cqlRows;
		this.rowKey = rowKey;
	}

	@Override
	public Tuple get(RowKey column) {
		if ( size() != 0 ) {
			//TODO only for single pk
			return new Tuple( new CassandraTupleSnapshot( this.cqlRows, this.associationKey.getEntityKey() ) );
		}
		else {
			return null;
		}
	}

	@Override
	public boolean containsKey(RowKey column) {
		return this.cqlRows.contains( column );
	}

	@Override
	public int size() {
		return this.cqlRows.size();
	}

	@Override
	public Set<RowKey> getRowKeys() {
		HashSet<RowKey> rowKeys = new HashSet<RowKey>();
		rowKeys.add( this.rowKey );
		return rowKeys;
	}
}
