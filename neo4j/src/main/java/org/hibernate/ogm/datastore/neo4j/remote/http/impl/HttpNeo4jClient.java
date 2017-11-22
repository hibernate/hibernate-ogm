/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.impl;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.remote.common.impl.RemoteNeo4jConfiguration;
import org.hibernate.ogm.datastore.neo4j.remote.common.impl.RemoteNeo4jDatabaseIdentifier;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.StatementsResponse;
import org.hibernate.ogm.datastore.neo4j.remote.http.request.impl.HttpNeo4jAuthenticationFacade;
import org.hibernate.ogm.datastore.neo4j.remote.http.request.impl.HttpNeo4jTransactionFacade;
import org.hibernate.ogm.datastore.neo4j.remote.http.request.impl.XStreamRequestHeaderFilter;
import org.hibernate.ogm.datastore.neo4j.remote.http.transaction.impl.HttpNeo4jTransaction;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * Access point to the remote Neo4j server.
 *
 * @author Davide D'Alto
 */
public class HttpNeo4jClient implements AutoCloseable {

	private static final int OK_STATUS_CODE = Response.Status.OK.getStatusCode();

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );



	/**
	 * Client for accessing the server
	 */
	private final ResteasyClient client;

	private final HttpNeo4jAuthenticationFacade authenticationClient;

	private final HttpNeo4jTransactionFacade neo4jFacade;

	private final RemoteNeo4jDatabaseIdentifier database;

	public HttpNeo4jClient(RemoteNeo4jDatabaseIdentifier database, RemoteNeo4jConfiguration configuration) {
		this.database = database;
		this.client = createRestClient( database, configuration );
		this.authenticationClient = client.target( database.getServerUri() ).proxy( HttpNeo4jAuthenticationFacade.class );
		this.neo4jFacade = client.target( database.getDatabaseUri() ).proxy( HttpNeo4jTransactionFacade.class );
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
		return clientBuilder.connectionPoolSize( configuration.getClientPoolSize() ).build();
	}

	public void validateConnection() {
		Response response = authenticationClient.authenticate( database.getUserName() );
		if ( response.getStatus() != OK_STATUS_CODE ) {
			throw log.connectionFailed( String.valueOf( database.getHost() ), String.valueOf( response.getStatus() ), response.getStatusInfo().getReasonPhrase() );
		}
	}

	/**
	 * Release all the resources
	 */
	@Override
	public void close() {
		client.close();
	}

	public StatementsResponse executeQueriesInOpenTransaction(Long txId, Statements statements) {
		Response response = neo4jFacade.executeQuery( txId, statements );
		try {
			return response.readEntity( StatementsResponse.class );
		}
		finally {
			response.close();
		}
	}

	public StatementsResponse executeQueriesInNewTransaction(Statements statements) {
		Response response = neo4jFacade.executeQuery( statements );
		try {
			return response.readEntity( StatementsResponse.class );
		}
		finally {
			response.close();
		}
	}

	public HttpNeo4jTransaction beginTx() {
		Response response = neo4jFacade.beginTransaction();
		try {
			Long txId = transactionId( response.getLocation() );
			HttpNeo4jTransaction transaction = new HttpNeo4jTransaction( this, txId );
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
