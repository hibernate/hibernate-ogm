/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.impl;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.map.impl.MapTupleSnapshot;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;
import org.infinispan.query.dsl.Query;

/**
 * Iterates over the result of an Infinispan query, when each result is a <b>partial</b> cache entry.
 * This is the case when the result of query is mapped from a raw type.
 * We have raw type result set in case of projection.
 * The result will be a single or a List of {@link Object[]}.
 *
 * @author Fabio Massimo Ercoli
 */
public class RawTypeClosableIterator implements ClosableIterator<Tuple> {

	private final Iterator<?> queryResultIterator;
	private final String[] projections;

	public RawTypeClosableIterator(Query query, String[] projections) {
		this.queryResultIterator = query.list().iterator();
		// Note that we are not using query.getProjection() because at
		// QueryFactory.create( ) doesn't populate the projection field
		this.projections = projections;
	}

	@Override
	public boolean hasNext() {
		return queryResultIterator.hasNext();
	}

	@Override
	public Tuple next() {
		Object[] rawType = (Object[]) queryResultIterator.next();
		Map<String, Object> map = new LinkedHashMap<>();
		for ( int i = 0; i < projections.length; i++ ) {
			map.put( projections[i], rawType[i] );
		}

		return new Tuple( new MapTupleSnapshot( map ), Tuple.SnapshotType.UPDATE );
	}

	@Override
	public void close() {
		// nothing to close
	}
}
