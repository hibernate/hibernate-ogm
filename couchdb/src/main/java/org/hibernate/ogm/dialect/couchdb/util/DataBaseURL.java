/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.couchdb.util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represent the URL pointing to an instance of the CouchDB
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class DataBaseURL {

	private static final String PROTOCOL = "http";
	private static final String SLASH = "/";

	private URL databaseURL;

	public DataBaseURL(String host, int port, String databaseName) throws MalformedURLException {
		databaseURL = new URL( PROTOCOL, host, port, SLASH + databaseName );
	}

	/**
	 * The name of the database
	 *
	 * @return the name of the database
	 */
	public String getDataBaseName() {
		return databaseURL.getPath().substring( 1 );
	}

	/**
	 * The server URL
	 *
	 * @return the server Url
	 */
	public String getServerUrl() {
		return toString().replace( SLASH + getDataBaseName(), "" );
	}

	@Override
	public String toString() {
		return databaseURL.toString();
	}

}
