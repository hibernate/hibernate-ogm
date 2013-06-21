/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.couchdb.model;

import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBAssociation;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.RowKey;

import java.util.Set;

/**
 * @author Andrea Boriero <dreborier@gmail.com>
 */
public class CouchDBAssociationSnapshot implements AssociationSnapshot {

	private CouchDBAssociation association;
	private AssociationKey key;

	public CouchDBAssociationSnapshot(CouchDBAssociation association, AssociationKey key) {
		this.association = association;
		this.key = key;
	}

	@Override
	public boolean containsKey(RowKey column) {
		return association.containsKey( column );
	}

	@Override
	public Tuple get(RowKey column) {
		return association.getTuple( column );
	}

	@Override
	public int size() {
		return association.size();
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return association.getRowKeys( key );
	}

}
