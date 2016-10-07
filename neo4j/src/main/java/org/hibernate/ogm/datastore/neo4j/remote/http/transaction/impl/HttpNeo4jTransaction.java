/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.transaction.impl;

import java.io.Closeable;

import org.hibernate.ogm.datastore.neo4j.remote.http.impl.HttpNeo4jClient;

/**
 * @author Davide D'Alto
 */
public class HttpNeo4jTransaction implements Closeable {

	private final Long txId;
	private final HttpNeo4jClient remote;

	public HttpNeo4jTransaction(HttpNeo4jClient remote, Long txId) {
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

	@Override
	public void close() {
	}
}
