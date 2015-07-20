/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchbase.util.impl;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
public class DatabaseIdentifier {
	private static final String PROTOCOL = "http";
	private static final String SLASH = "/";
	private final String host;
	private final int port;
	private final String databaseName;
	private final String password;
	private final URI serverUri;
	private final URI databaseUri;
	public DatabaseIdentifier(String host, int port, String databaseName, String password)
					throws MalformedURLException, URISyntaxException {
		this.host = host;
		this.port = port;
		this.databaseName = databaseName;
		this.password = password;
		serverUri = new URL( PROTOCOL, host, port, "" ).toURI();
		databaseUri = new URL( PROTOCOL, host, port, SLASH + databaseName ).toURI();
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
	 * Returns the URI of the CouchBase server, e.g. "http://localhost:8091/index.html".
	 *
	 * @return the URI of the CouchBase server
	 */
	public URI getServerUri() {
		return serverUri;
	}
	/**
	 * Returns the URI of the database, e.g. "http://localhost:8091/...".
	 *
	 * @return the URI of the database
	 */
	public URI getDatabaseUri() {
		return databaseUri;
	}
	public String getPassword() {
		return password;
	}
	@Override
	public String toString() {
		return "DatabaseIdentifier [host=" + host + ", port=" + port + ", databaseName=" + databaseName +
						", password=***" + ", serverUri=" + serverUri + ", databaseUri=" + databaseUri + "]";
	}
}
