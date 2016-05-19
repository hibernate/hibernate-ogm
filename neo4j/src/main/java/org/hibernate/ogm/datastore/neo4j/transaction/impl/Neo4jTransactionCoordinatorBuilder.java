/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.transaction.impl;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Builder for {@link Neo4jJtaTransactionCoordinator}.
 *
 * @author Davide D'Alto
 */
public class Neo4jTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {

	private final Neo4jDatastoreProvider datastoreProvider;
	private final TransactionCoordinatorBuilder delegate;

	public Neo4jTransactionCoordinatorBuilder(TransactionCoordinatorBuilder delegate, Neo4jDatastoreProvider datastoreProvider) {
		this.delegate = delegate;
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorOptions options) {
		if ( delegate.isJta() ) {
			TransactionCoordinator coordinator = delegate.buildTransactionCoordinator( owner, options );
			return new Neo4jJtaTransactionCoordinator( coordinator, datastoreProvider );
		}
		else {
			return new Neo4jResourceLocalTransactionCoordinator( this, owner, datastoreProvider );
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
