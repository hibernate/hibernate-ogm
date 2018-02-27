/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.bolt.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.neo4j.remote.common.impl.RemoteNeo4jConfiguration;
import org.hibernate.ogm.datastore.neo4j.remote.common.impl.RemoteNeo4jDatabaseIdentifier;

import org.neo4j.driver.v1.AuthToken;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.exceptions.Neo4jException;

/**
 * @author Davide D'Alto
 */
public class BoltNeo4jClient {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final Driver driver;

	private final String databaseUri;

	public BoltNeo4jClient(RemoteNeo4jDatabaseIdentifier identifier, RemoteNeo4jConfiguration configuration) {
		this.databaseUri = identifier.getDatabaseUri();
		this.driver = createNeo4jDriver( identifier, configuration );
	}

	private Driver createNeo4jDriver(RemoteNeo4jDatabaseIdentifier identifier, RemoteNeo4jConfiguration configuration) {
		String uri = identifier.getDatabaseUri();
		try {
			if ( configuration.isAuthenticationRequired() ) {
				AuthToken authToken = AuthTokens.basic( configuration.getUsername(), configuration.getPassword() );
				return GraphDatabase.driver( uri, authToken );
			}
			else {
				return GraphDatabase.driver( uri );
			}
		}
		catch (Neo4jException e) {
			throw log.connectionFailed( databaseUri, e.code(), e.getMessage() );
		}
	}

	public void close() {
		if ( driver != null ) {
			driver.close();
		}
	}

	public Driver getDriver() {
		return driver;
	}
}
