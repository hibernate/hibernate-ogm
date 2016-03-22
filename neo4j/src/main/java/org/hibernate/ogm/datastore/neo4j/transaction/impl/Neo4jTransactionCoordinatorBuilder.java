/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.transaction.impl;

import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.transaction.emulated.impl.NoopJdbcResourceTransactionCoordinatorOwner;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Builder for {@link Neo4jJtaTransactionCoordinator}.
 *
 * @author Davide D'Alto
 */
public class Neo4jTransactionCoordinatorBuilder extends ForwardingTransactionCoordinatorBuilder {

	private final Neo4jDatastoreProvider datastoreProvider;

	public Neo4jTransactionCoordinatorBuilder(TransactionCoordinatorBuilder delegate, Neo4jDatastoreProvider datastoreProvider) {
		super( delegate );
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorOptions options) {
		if ( isJta() ) {
			TransactionCoordinator coordinator = super.buildTransactionCoordinator( owner, options );
			return new Neo4jJtaTransactionCoordinator( coordinator, datastoreProvider );
		}
		else {
			TransactionCoordinatorOwner noopedOwner = new NoopJdbcResourceTransactionCoordinatorOwner( owner );
			TransactionCoordinator coordinator = super.buildTransactionCoordinator( noopedOwner, options );
			return new Neo4jLocalTransactionCoordinator( coordinator, datastoreProvider );
		}
	}
}
