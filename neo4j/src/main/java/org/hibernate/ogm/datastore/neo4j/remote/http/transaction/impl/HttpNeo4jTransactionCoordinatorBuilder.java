/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.transaction.impl;

import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jDatastoreProvider;
import org.hibernate.ogm.transaction.impl.ForwardingTransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinator;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;

/**
 * Builder for a Neo4j remote {@link TransactionCoordinator}.
 *
 * @author Davide D'Alto
 */
public class HttpNeo4jTransactionCoordinatorBuilder extends ForwardingTransactionCoordinatorBuilder {

	private final HttpNeo4jDatastoreProvider datastoreProvider;

	public HttpNeo4jTransactionCoordinatorBuilder(TransactionCoordinatorBuilder delegate, HttpNeo4jDatastoreProvider datastoreProvider) {
		super( delegate );
		this.datastoreProvider = datastoreProvider;
	}

	@Override
	public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, Options options) {
		if ( isJta() ) {
			TransactionCoordinator coordinator = super.buildTransactionCoordinator( owner, options );
			return new HttpNeo4jJtaTransactionCoordinator( coordinator, datastoreProvider );
		}
		else {
			return new HttpNeo4jResourceLocalTransactionCoordinator( this, owner, datastoreProvider );
		}
	}
}
