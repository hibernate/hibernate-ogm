/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.transaction.impl;

import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionImplementor;
import org.neo4j.graphdb.Transaction;

/**
 * @author Davide D'Alto
 */
public class Neo4jTransactionImplementor extends ForwardingTransactionImplementor {

	private final Neo4jDatastoreProvider datastoreProvider;
	private Transaction tx;

	public Neo4jTransactionImplementor(TransactionImplementor delegate, Neo4jDatastoreProvider datastoreProvider) {
		super( delegate );
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public void begin() {
		tx = datastoreProvider.getDataBase().beginTx();
		super.begin();
	}

	@Override
	public void commit() {
		super.commit();
		success();
	}

	@Override
	public void rollback() {
		super.rollback();
		failure();
	}

	@Override
	public void join() {
		super.join();
	}

	@Override
	protected TransactionImplementor getDelegate() {
		return super.getDelegate();
	}

	private void success() {
		try {
			tx.success();
		}
		finally {
			tx.close();
		}
	}

	private void failure() {
		try {
			tx.failure();
		}
		finally {
			tx.close();
		}
	}
}
