/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.transaction.impl;

import org.hibernate.resource.transaction.spi.TransactionCoordinator;

/**
 * Groups commons method used by the different Neo4j transaction coordinators in a JTA environment.
 *
 * @author Davide D'Alto
 */
public interface BaseNeo4jJtaTransactionCoordinator extends TransactionCoordinator {

	void join();
	void success();
	void failure();

	/**
	 * @return if the transaction is not null (does not check the status)
	 */
	boolean isTransactionOpen();
	void beginTransaction();
	Object getTransactionId();
}
