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
package org.hibernate.ogm.dialect.couchdb;

/**
 * CouchDBConfiguration options of the CouchDB GridDialect
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public interface Environment {

	/**
	 * The CouchDB Database name to connect to.
	 */
	String COUCHDB_DATABASE = "hibernate.ogm.couchdb.database";

	/**
	 * The port of the CouchDB instance.
	 */
	String COUCHDB_PORT = "hibernate.ogm.couchdb.port";

	/**
	 * The hostname of the CouchDB instance.
	 */
	String COUCHDB_HOST = "hibernate.ogm.couchdb.host";

	/**
	 * The username of the CouchDB database user with authentication enabled.
	 */
	String COUCHDB_USERNAME = "hibernate.org.couchdb.username";

	/**
	 * The username of the CouchDB database user with authentication enabled.
	 */
	String COUCHDB_PASSWORD = "hibernate.org.couchdb.password";

	/**
	 * If the database does not exist set this value to true.
	 *
	 * Default value is false
	 *
	 * If COUCHDB_USERNAME is provided and COUCHDB_CREATE_DATABASE value is set to true, be sure the user has the
	 * privileges to create a
	 * new database
	 */
	String COUCHDB_CREATE_DATABASE = "hibernate.org.couchdb.createdatabase";

}
