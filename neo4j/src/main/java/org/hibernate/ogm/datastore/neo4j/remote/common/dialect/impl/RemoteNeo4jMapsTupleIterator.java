/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.common.dialect.impl;

import java.util.Iterator;
import java.util.List;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.hibernate.ogm.model.spi.TupleSnapshot;

/**
 * @author Davide D'Alto
 */
public abstract class RemoteNeo4jMapsTupleIterator<T> implements ClosableIterator<Tuple> {

	private final Iterator<T> iterator;
	private final List<String> columns;

	public RemoteNeo4jMapsTupleIterator(Iterator<T> iterator, List<String> columns) {
		this.iterator = iterator;
		this.columns = columns;
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public Tuple next() {
		T row = iterator.next();
		return new Tuple( tupleSnapshot( row, columns ), SnapshotType.UPDATE );
	}

	protected abstract TupleSnapshot tupleSnapshot(T next, List<String> columns);

	@Override
	public void remove() {
		iterator.remove();
	}

	@Override
	public void close() {
	}
}
