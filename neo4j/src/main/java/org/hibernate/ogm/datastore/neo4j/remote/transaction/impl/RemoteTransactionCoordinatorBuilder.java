/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.transaction.impl;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jDatastoreProvider;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Builder for {@link Neo4jTransactionCoordinator}.
 *
 * @author Davide D'Alto
 */
public class RemoteTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {

	private final TransactionCoordinatorBuilder delegate;
	private final RemoteNeo4jDatastoreProvider datastoreProvider;

	public RemoteTransactionCoordinatorBuilder(TransactionCoordinatorBuilder delegate, RemoteNeo4jDatastoreProvider datastoreProvider) {
		this.delegate = delegate;
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorOptions options) {
		TransactionCoordinator coordinator = delegate.buildTransactionCoordinator( owner, options );
		if ( delegate.isJta() ) {
			return new RemoteJtaTransactionCoordinator( coordinator, datastoreProvider );
		}
		else {
			return new RemoteLocalTransactionCoordinator( coordinator, datastoreProvider );
		}

	}

	@Override
	public boolean isJta() {
		return delegate.isJta();
	}

	@Override
	public ConnectionReleaseMode getDefaultConnectionReleaseMode() {
		return delegate.getDefaultConnectionReleaseMode();
	}

	@Override
	public ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode() {
		return delegate.getDefaultConnectionAcquisitionMode();
	}
}
