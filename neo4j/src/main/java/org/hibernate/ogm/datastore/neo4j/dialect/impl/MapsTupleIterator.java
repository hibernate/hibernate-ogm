/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.neo4j.dialect.impl;

import java.util.Map;

import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.dialect.TupleIterator;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.ResourceIterator;

/**
 * Iterates over the results of a native query when each result is not mapped by an entity
 */
public class MapsTupleIterator implements TupleIterator {

	private final ResourceIterator<Map<String, Object>> iterator;

	public MapsTupleIterator(ExecutionResult result) {
		this.iterator = result.iterator();
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public Tuple next() {
		return convert( iterator.next() );
	}

	protected Tuple convert(Map<String, Object> next) {
		return new Tuple( new MapTupleSnapshot( next ) );
	}

	@Override
	public void remove() {
		iterator.remove();
	}

	@Override
	public void close() {
		iterator.close();
	}
}
