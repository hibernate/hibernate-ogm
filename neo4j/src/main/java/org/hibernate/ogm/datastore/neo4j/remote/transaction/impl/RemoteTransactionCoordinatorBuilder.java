/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.transaction.impl;

import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jDatastoreProvider;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Builder for a Neo4j remote {@link TransactionCoordinator}.
 *
 * @author Davide D'Alto
 */
public class RemoteTransactionCoordinatorBuilder extends ForwardingTransactionCoordinatorBuilder {

	private final RemoteNeo4jDatastoreProvider datastoreProvider;

	public RemoteTransactionCoordinatorBuilder(TransactionCoordinatorBuilder delegate, RemoteNeo4jDatastoreProvider datastoreProvider) {
		super( delegate );
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorOptions options) {
		if ( isJta() ) {
			TransactionCoordinator coordinator = super.buildTransactionCoordinator( owner, options );
			return new RemoteJtaTransactionCoordinator( coordinator, datastoreProvider );
		}
		else {
			return new RemoteResourceLocalTransactionCoordinator( this, owner, datastoreProvider );
		}
	}
}
