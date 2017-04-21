/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.transaction.impl;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.ogm.datastore.orientdb.impl.OrientDBDatastoreProvider;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Builder for TransactionCoordinator instances
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class OrientDbTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {

	private final TransactionCoordinatorBuilder delegate;
	private final OrientDBDatastoreProvider datastoreProvider;

	/**
	 * Constructor
	 *
	 * @param delegate builder of transaction coordinator
	 * @param datastoreProvider OrientDB datastore provider
	 */
	public OrientDbTransactionCoordinatorBuilder(TransactionCoordinatorBuilder delegate, OrientDBDatastoreProvider datastoreProvider) {
		this.delegate = delegate;
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorOptions options) {
		TransactionCoordinator coordinator = delegate.buildTransactionCoordinator( owner, options );
		if ( delegate.isJta() ) {
			return new OrientDBJtaTransactionCoordinator( coordinator, datastoreProvider );
		}
		else {
			return new OrientDBLocalTransactionCoordinator( coordinator, datastoreProvider );
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
