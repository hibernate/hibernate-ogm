/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.transaction.impl;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.resource.transaction.spi.TransactionStatus;

/**
 * {@link Synchronization} implementation for Neo4j.
 *
 * @author Davide D'Alto
 */
public class Neo4jSynchronization implements Synchronization {

	private BaseNeo4jJtaTransactionCoordinator delegate;

	public Neo4jSynchronization(BaseNeo4jJtaTransactionCoordinator transactionCoordinator) {
		this.delegate = transactionCoordinator;
	}

	@Override
	public void beforeCompletion() {
		TransactionStatus status = delegate.getTransactionDriverControl().getStatus();
		if ( status == TransactionStatus.MARKED_ROLLBACK ) {
			delegate.failure();
		}
		else {
			delegate.success();
		}
	}

	@Override
	public void afterCompletion(int status) {
		if ( delegate.isTransactionOpen() ) {
			if ( status != Status.STATUS_COMMITTED ) {
				delegate.failure();
			}
			else {
				delegate.success();
			}
		}
	}
}
