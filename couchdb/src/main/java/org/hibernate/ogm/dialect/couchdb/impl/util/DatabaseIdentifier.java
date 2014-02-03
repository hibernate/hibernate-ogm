/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.dialect.couchdb.impl.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Provides all information required to connect to a CouchDB database.
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 * @author Gunnar Morling
 */
public class DatabaseIdentifier {

	private static final String PROTOCOL = "http";
	private static final String SLASH = "/";

	private final String host;
	private final int port;
	private final String databaseName;
	private final String userName;
	private final String password;

	private final URI serverUri;
	private final URI databaseUri;

	public DatabaseIdentifier(String host, int port, String databaseName, String userName, String password) throws MalformedURLException, URISyntaxException {
		this.host = host;
		this.port = port;
		this.databaseName = databaseName;
		this.userName = userName;
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
	 * Returns the URI of the CouchDB server, e.g. "http:://localhost:5984".
	 *
	 * @return the URI of the CouchDB server
	 */
	public URI getServerUri() {
		return serverUri;
	}

	/**
	 * Returns the URI of the database, e.g. "http:://localhost:5984/mydb".
	 *
	 * @return the URI of the database
	 */
	public URI getDatabaseUri() {
		return databaseUri;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		return "DatabaseIdentifier [host=" + host + ", port=" + port + ", databaseName=" + databaseName + ", userName=" + userName + ", password=***"
				+ ", serverUri=" + serverUri + ", databaseUri=" + databaseUri + "]";
	}
}
