/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.impl;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import org.hibernate.ogm.datastore.neo4j.remote.facade.impl.RemoteNeo4jAuthenticationFacade;
import org.hibernate.ogm.datastore.neo4j.remote.facade.impl.RemoteNeo4jTransactionFacade;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
import org.hibernate.ogm.datastore.neo4j.remote.transaction.impl.RemoteNeo4jTransaction;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * Access point to the remote Neo4j server.
 *
 * @author Davide D'Alto
 */
public class RemoteNeo4jClient implements AutoCloseable {

	/**
	 * Size of the client connection pool used by the RestEasy HTTP client
	 */
	private static final int CONNECTION_POOL_SIZE = 10;

	/**
	 * Client for accessing the server
	 */
	private final ResteasyClient client;

	private final RemoteNeo4jAuthenticationFacade authenticationClient;

	private final RemoteNeo4jTransactionFacade neo4jFacade;

	public RemoteNeo4jClient(RemoteNeo4jDatabaseIdentifier database, RemoteNeo4jConfiguration configuration) {
		this.client = createRestClient( database, configuration );
		this.authenticationClient = client.target( database.getServerUri() ).proxy( RemoteNeo4jAuthenticationFacade.class );
		this.neo4jFacade = client.target( database.getDatabaseUri() ).proxy( RemoteNeo4jTransactionFacade.class );
	}

	private static ResteasyClient createRestClient(RemoteNeo4jDatabaseIdentifier database, RemoteNeo4jConfiguration configuration) {
		ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();

		if ( database.getUserName() != null ) {
			clientBuilder.register( new BasicAuthentication( database.getUserName(), database.getPassword() ) );
		}

		if ( configuration.getConnectionCheckoutTimeout() != null ) {
			clientBuilder.connectionCheckoutTimeout( configuration.getConnectionCheckoutTimeout(), TimeUnit.MILLISECONDS );
		}

		if ( configuration.getEstablishConnectionTimeout() != null ) {
			clientBuilder.establishConnectionTimeout( configuration.getEstablishConnectionTimeout(), TimeUnit.MILLISECONDS );
		}

		if ( configuration.getConnectionTTL() != null ) {
			clientBuilder.connectionTTL( configuration.getConnectionTTL(), TimeUnit.MILLISECONDS );
		}

		if ( configuration.getSocketTimeout() != null ) {
			clientBuilder.socketTimeout( configuration.getSocketTimeout(), TimeUnit.MILLISECONDS );
		}

		clientBuilder.register( XStreamRequestHeaderFilter.INSTANCE );

		// using a connection pool size > 1 causes a thread-safe pool implementation to be used under the hoods
		return clientBuilder.connectionPoolSize( CONNECTION_POOL_SIZE ).build();
	}

	public Response authenticate(String username) {
		return authenticationClient.authenticate( username );
	}

	/**
	 * Release all the resources
	 */
	public void close() {
		client.close();
	}

	public StatementsResponse executeQueriesInOpenTransaction(Long txId, Statements statements) {
		Response executeQuery = neo4jFacade.executeQuery( txId, statements );
		try {
			return executeQuery.readEntity( StatementsResponse.class );
		}
		finally {
			executeQuery.close();
		}
	}

	public StatementsResponse executeQueriesInNewTransaction(Statements statements) {
		Response response = neo4jFacade.executeQuery( statements );
		try {
			StatementsResponse readEntity = response.readEntity( StatementsResponse.class );
			return readEntity;
		}
		finally {
			response.close();
		}
	}

	public RemoteNeo4jTransaction beginTx() {
		Response response = neo4jFacade.beginTransaction();
		try {
			Long txId = transactionId( response.getLocation() );
			RemoteNeo4jTransaction transaction = new RemoteNeo4jTransaction( this, txId );
			return transaction;
		}
		finally {
			response.close();
		}
	}

	// The location should look something like: http://localhost:7474/db/data/transaction/{txId}
	private Long transactionId(URI location) {
		return Long.valueOf( location.getPath().substring( location.getPath().lastIndexOf( "/" ) + 1 ) );
	}

	public void commit(Long txId) {
		Response response = neo4jFacade.commit( txId );
		response.close();
	}

	public void rollback(Long txId) {
		Response response = neo4jFacade.rollback( txId );
		response.close();
	}
}
