/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.query.impl;

import java.util.Iterator;
import java.util.List;

import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamPayload;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.model.spi.Tuple;

/**
 * Iterates over the result of an Infinispan query, when each result is a **full** cache entry.
 * This is the case when the result of query is mapped from a Protostream payload type.
 *
 * @author Fabio Massimo Ercoli
 */
public class ProtostreamPayloadClosableIterator implements ClosableIterator<Tuple> {

	private final Iterator<ProtostreamPayload> delegate;

	public ProtostreamPayloadClosableIterator(List<ProtostreamPayload> queryResult) {
		this.delegate = queryResult.iterator();
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public Tuple next() {
		return delegate.next().toTuple( Tuple.SnapshotType.UPDATE );
	}

	@Override
	public void close() {
		// nothing to close
	}
}
