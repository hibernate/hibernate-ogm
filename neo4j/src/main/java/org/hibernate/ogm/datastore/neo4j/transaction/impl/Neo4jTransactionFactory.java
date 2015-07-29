/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.transaction.impl;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionImplementor;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;

/**
 * @author Davide D'Alto
 */
public class Neo4jTransactionFactory implements TransactionFactory<Neo4jTransactionImplementor> {

	private final TransactionFactory<?> delegate;
	private final Neo4jDatastoreProvider datastoreProvider;
	private final JtaPlatform jtaPlatform;

	public Neo4jTransactionFactory(TransactionFactory<?> delegate, Neo4jDatastoreProvider datastoreProvider, JtaPlatform jtaPlatform) {
		this.delegate = delegate;
		this.datastoreProvider = datastoreProvider;
		this.jtaPlatform = jtaPlatform;
	}

	@Override
	public Neo4jTransactionImplementor createTransaction(TransactionCoordinator coordinator) {
		TransactionImplementor transaction = delegate.createTransaction( coordinator );
		return new Neo4jTransactionImplementor( transaction, datastoreProvider, jtaPlatform );
	}

	@Override
	public boolean canBeDriver() {
		return delegate.canBeDriver();
	}

	@Override
	public boolean compatibleWithJtaSynchronization() {
		return delegate.compatibleWithJtaSynchronization();
	}

	@Override
	public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator, Neo4jTransactionImplementor transaction) {
		return ( (TransactionFactory<TransactionImplementor>) delegate ).isJoinableJtaTransaction( transactionCoordinator, transaction.getDelegate() );
	}

	@Override
	public ConnectionReleaseMode getDefaultReleaseMode() {
		return delegate.getDefaultReleaseMode();
	}

}
