/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;
import org.neo4j.graphdb.ResourceIterator;

/**
 * Super class for the iterators used by the embedded dialect in Nero4j.
 *
 * @author Davide D'Alto
 */
public abstract class EmbeddedNeo4jTupleIterator<T> implements ClosableIterator<Tuple> {

	private final ResourceIterator<T> iterator;

	public EmbeddedNeo4jTupleIterator(ResourceIterator<T> result) {
		this.iterator = result;
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public Tuple next() {
		return convert( iterator.next() );
	}

	protected abstract Tuple convert(T next);

	@Override
	public void remove() {
		iterator.remove();
	}

	@Override
	public void close() {
		iterator.close();
	}
}
