/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.impl;

import java.util.Iterator;
import java.util.List;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleSupplier;
import org.hibernate.ogm.model.spi.Tuple;


/**
 * @author Davide D'Alto
 */
public class CouchDBTupleSupplier implements TupleSupplier {

	private final List<Tuple> tuples;

	public CouchDBTupleSupplier(List<Tuple> tuples) {
		this.tuples = tuples;
	}

	@Override
	public ClosableIterator<Tuple> get(TransactionContext transactionContext) {
		return new TupleIterator( tuples.iterator() );
	}

	private static class TupleIterator implements ClosableIterator<Tuple> {

		private final Iterator<Tuple> iterator;

		public TupleIterator(Iterator<Tuple> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public Tuple next() {
			return iterator.next();
		}

		@Override
		public void close() {
		}
	}
}
