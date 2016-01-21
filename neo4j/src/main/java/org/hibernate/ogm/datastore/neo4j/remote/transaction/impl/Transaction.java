/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.transaction.impl;

import java.io.Closeable;

import org.hibernate.ogm.datastore.neo4j.remote.impl.Neo4jClient;

/**
 * @author Davide D'Alto
 */
public class Transaction implements Closeable {

	private final Long txId;
	private final Neo4jClient remote;

	public Transaction(Neo4jClient remote, Long txId) {
		this.txId = txId;
		this.remote = remote;
	}

	public void commit() {
		remote.commit( txId );
	}

	public void rollback() {
		remote.rollback( txId );
	}

	public Long getId() {
		return txId;
	}

	public void close() {
	}
}
