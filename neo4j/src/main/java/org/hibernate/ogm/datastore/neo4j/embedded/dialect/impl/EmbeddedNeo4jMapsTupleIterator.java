/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.dialect.impl;

import java.util.Map;

import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.model.spi.Tuple.SnapshotType;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;

/**
 * Iterates over the results of a native query when each result is not mapped by an entity
 *
 * @author Davide D'Alto
 */
public class EmbeddedNeo4jMapsTupleIterator implements ClosableIterator<Tuple> {

	private final ResourceIterator<Map<String, Object>> iterator;

	public EmbeddedNeo4jMapsTupleIterator(Result result) {
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

	protected Tuple convert(Map<String, Object> next) {
		return new Tuple( new MapTupleSnapshot( next ), SnapshotType.UPDATE );
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
