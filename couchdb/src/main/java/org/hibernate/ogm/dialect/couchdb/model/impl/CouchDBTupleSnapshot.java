/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.couchdb.model.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.EntityKey;

/**
 * A {@link TupleSnapshot} based on the properties of a CouchDB
 * {@link org.hibernate.ogm.dialect.couchdb.backend.json.EntityDocument}.
 * <p>
 * Fundamentally a {@link org.hibernate.ogm.datastore.impl.MapTupleSnapshot} except that the
 * {@link org.hibernate.ogm.grid.EntityKey} column names and values are copied.
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 * @author Gunnar Morling
 */
public class CouchDBTupleSnapshot implements TupleSnapshot {

	private final Map<String, Object> properties;
	private final boolean createdOnInsert;

	public CouchDBTupleSnapshot(EntityKey key) {
		createdOnInsert = true;

		properties = new HashMap<String, Object>();
		for ( int i = 0; i < key.getColumnNames().length; i++ ) {
			properties.put( key.getColumnNames()[i], key.getColumnValues()[i] );
		}
	}

	public CouchDBTupleSnapshot(Map<String, Object> properties) {
		createdOnInsert = false;
		this.properties = properties;
	}

	@Override
	public Object get(String column) {
		return properties.get( column );
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public Set<String> getColumnNames() {
		return properties.keySet();
	}

	/**
	 * Whether this snapshot has been created during an insert or not.
	 *
	 * @return {@code true} if the snapshot has been created during an insert, {@code false} if it has been created
	 * during an update.
	 */
	public boolean isCreatedOnInsert() {
		return createdOnInsert;
	}
}
