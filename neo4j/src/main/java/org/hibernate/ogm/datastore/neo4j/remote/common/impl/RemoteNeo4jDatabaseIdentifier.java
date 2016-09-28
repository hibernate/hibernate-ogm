/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.common.impl;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import org.hibernate.ogm.cfg.spi.Hosts.HostAndPort;

/**
 * Identify a db connection.
 * <p>
 * Example: http://locahost:8080/db/data
 *
 * @author Davide D'Alto
 */
public class RemoteNeo4jDatabaseIdentifier {

	private static final String SLASH = "/";

	private final String host;
	private final int port;
	private final String databaseName;
	private final String username;
	private final String password;

	private final String serverUri;
	private final String databaseUri;

	public RemoteNeo4jDatabaseIdentifier(String protocol, RemoteNeo4jConfiguration configuration)
			throws MalformedURLException, URISyntaxException {
		HostAndPort first = configuration.getHosts().getFirst();
		this.host = first.getHost();
		this.port = first.getPort();
		this.databaseName = configuration.getDatabaseName();
		this.username = configuration.getUsername();
		this.password = configuration.getPassword();

		this.serverUri = protocol + "://" + host + ":" + port;
		this.databaseUri = serverUri + SLASH + databaseName;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	/**
	 * The name of the database
	 *
	 * @return the name of the database
	 */
	public String getDatabaseName() {
		return databaseName;
	}

	/**
	 * Returns the URI of the Neo4j server, e.g. "http://localhost:5984".
	 *
	 * @return the URI of the Neo4j server as string
	 */
	public String getServerUri() {
		return serverUri;
	}

	/**
	 * Returns the URI of the database, e.g. "http://localhost:5984/mydb".
	 *
	 * @return the URI of the database as string
	 */
	public String getDatabaseUri() {
		return databaseUri;
	}

	public String getUserName() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		return "RemoteNeo4jDatabaseIdentifier [host=" + host + ", port=" + port + ", databaseName=" + databaseName + ", userName=" + username + ", password=***"
				+ ", serverUri=" + serverUri + ", databaseUri=" + databaseUri + "]";
	}
}
