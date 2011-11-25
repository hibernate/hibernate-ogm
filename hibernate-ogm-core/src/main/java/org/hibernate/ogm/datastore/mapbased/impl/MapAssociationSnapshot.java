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
package org.hibernate.ogm.datastore.mapbased.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.impl.MapBasedTupleSnapshot;
import org.hibernate.ogm.datastore.spi.AssociationSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.RowKey;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class MapAssociationSnapshot implements AssociationSnapshot {

	private final Map<RowKey, Map<String, Object>> associationMap;

	public MapAssociationSnapshot(Map<RowKey, Map<String, Object>> associationMap) {
		this.associationMap = associationMap;
	}

	@Override
	public Tuple get(RowKey column) {
		Map<String, Object> rawResult = associationMap.get( column );
		return rawResult != null ? new Tuple( new MapBasedTupleSnapshot( rawResult ) ) : null;
	}

	@Override
	public boolean containsKey(RowKey column) {
		return associationMap.containsKey( column );
	}

	@Override
	public int size() {
		return associationMap.size();
	}

	@Override
	public Set<RowKey> getRowKeys() {
		return associationMap.keySet();
	}

	public Map<RowKey, Map<String, Object>> getUnderlyingMap() {
		return associationMap;
	}

}
