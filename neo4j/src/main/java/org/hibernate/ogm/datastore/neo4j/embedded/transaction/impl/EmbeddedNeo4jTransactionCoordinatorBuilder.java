/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.transaction.impl;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.ogm.datastore.neo4j.embedded.impl.EmbeddedNeo4jDatastoreProvider;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Builder for {@link EmbeddedNeo4jJtaTransactionCoordinator}.
 *
 * @author Davide D'Alto
 */
public class EmbeddedNeo4jTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {

	private final EmbeddedNeo4jDatastoreProvider datastoreProvider;
	private final TransactionCoordinatorBuilder delegate;

	public EmbeddedNeo4jTransactionCoordinatorBuilder(TransactionCoordinatorBuilder delegate, EmbeddedNeo4jDatastoreProvider datastoreProvider) {
		this.delegate = delegate;
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
		if ( delegate.isJta() ) {
			TransactionCoordinator coordinator = delegate.buildTransactionCoordinator( owner, options );
			return new EmbeddedNeo4jJtaTransactionCoordinator( coordinator, datastoreProvider );
		}
		else {
			return new EmbeddedNeo4jResourceLocalTransactionCoordinator( this, owner, datastoreProvider );
		}
	}

	@Override
	public boolean isJta() {
		return delegate.isJta();
	}

	@SuppressWarnings("deprecation")
	@Override
	public ConnectionReleaseMode getDefaultConnectionReleaseMode() {
		return delegate.getDefaultConnectionReleaseMode();
	}

	@SuppressWarnings("deprecation")
	@Override
	public ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode() {
		return delegate.getDefaultConnectionAcquisitionMode();
	}

	@Override
	public PhysicalConnectionHandlingMode getDefaultConnectionHandlingMode() {
		return delegate.getDefaultConnectionHandlingMode();
	}
}
