/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.impl;

import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.hibernate.ogm.datastore.neo4j.remote.facade.impl.Neo4jAuthenticationFacade;
import org.hibernate.ogm.datastore.neo4j.remote.facade.impl.Neo4jTransactionFacade;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statement;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.Statements;
import org.hibernate.ogm.datastore.neo4j.remote.json.impl.StatementsResponse;
import org.hibernate.ogm.datastore.neo4j.remote.transaction.impl.Transaction;
import org.hibernate.ogm.remote.impl.DatabaseIdentifier;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

/**
 * Access point to the remote Neo4j server.
 *
 * @author Davide D'Alto
 */
public class Neo4jClient implements AutoCloseable {

	/**
	 * Size of the client connection pool used by the RestEasy HTTP client
	 */
	private static final int CONNECTION_POOL_SIZE = 10;

	private final ThreadLocal<Long> transactionId = new ThreadLocal<>();

	/**
	 * Client for accessing the server
	 */
	private final ResteasyClient client;

	private final Neo4jAuthenticationFacade authenticationClient;

	private final Neo4jTransactionFacade neo4jFacade;

	public Neo4jClient(DatabaseIdentifier database) {
		this.client = createRestClient( database );
		this.authenticationClient = client.target( database.getServerUri() ).proxy( Neo4jAuthenticationFacade.class );
		this.neo4jFacade = client.target( database.getDatabaseUri() ).proxy( Neo4jTransactionFacade.class );
	}

	private static ResteasyClient createRestClient(DatabaseIdentifier database) {
		ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();

		if ( database.getUserName() != null ) {
			clientBuilder.register( new BasicAuthentication( database.getUserName(), database.getPassword() ) );
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

	public Response executeQuery(String query) {
		Statement statement = new Statement();
		statement.setStatement( query );

		Statements statements = new Statements();
		statements.setStatements( Arrays.asList( statement ) );

		return neo4jFacade.executeQuery( statements );
	}

	public Response executeQuery(String query, Map<String, Object> params) {
		Statement statement = new Statement();
		statement.setStatement( query );
		statement.setParameters( params );

		return executeQuery( statement );
	}

	public Response executeQuery(Statement statement) {
		Statements statements = new Statements();
		statements.setStatements( Arrays.asList( statement ) );

		if ( transactionId.get() != null ) {
			return neo4jFacade.executeQuery( transactionId.get(), statements );
		}
		else {
			return neo4jFacade.executeQuery( statements );
		}
	}

	public StatementsResponse executeQueriesInOpenTransaction(Statements statements) {
		Long id = transactionId.get();
		Response executeQuery = neo4jFacade.executeQuery( id, statements );
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

	public Transaction beginTx() {
		Response response = neo4jFacade.beginTransaction();
		try {
			URI location = response.getLocation();
			Transaction transaction = new Transaction( this, location );
			Long id = transaction.getId();
			transactionId.set( id );
			return transaction;
		}
		finally {
			response.close();
		}
	}

	public void commit(Long txId) {
		Long id = transactionId.get();
		if ( id != null ) {
			Response response = neo4jFacade.commit( id );
			try {
				transactionId.remove();
			}
			finally {
				response.close();
			}
		}
	}

	public void rollback(Long txId) {
		Long id = transactionId.get();
		if ( id != null ) {
			Response response = neo4jFacade.rollback( id );
			try {
				transactionId.remove();
			}
			finally {
				response.close();
			}
		}
	}
}
