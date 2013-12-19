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
package org.hibernate.ogm.datastore.couchdb;

import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.couchdb.mapping.CouchDBGlobalContext;
import org.hibernate.ogm.options.couchdb.mapping.impl.CouchDBEntityContextImpl;
import org.hibernate.ogm.options.couchdb.mapping.impl.CouchDBGlobalContextImpl;
import org.hibernate.ogm.options.couchdb.mapping.impl.CouchDBPropertyContextImpl;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;

/**
 * Allows to configure options specific to the CouchDB document data store.
 *
 * @author Gunnar Morling
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDB implements DatastoreConfiguration<CouchDBGlobalContext> {

	/**
	 * Name of the property for configuring the strategy for storing associations in CouchDB. Valid values are the
	 * string values of the members of the {@link org.hibernate.ogm.options.couchdb.AssociationStorageType} enumeration.
	 * Defaults to the in-entity storage strategy.
	 * <p>
	 * Note that any value specified via this property will be overridden by values configured via annotations or the
	 * programmatic API.
	 */
	public static final String ASSOCIATIONS_STORE = "hibernate.ogm.couchdb.associations.store";

	/**
	 * The CouchDB Database name to connect to.
	 */
	public static final String DATABASE = "hibernate.ogm.couchdb.database";

	/**
	 * The port of the CouchDB instance.
	 */
	public static final String PORT = "hibernate.ogm.couchdb.port";

	/**
	 * The hostname of the CouchDB instance.
	 */
	public static final String HOST = "hibernate.ogm.couchdb.host";

	/**
	 * The username of the CouchDB database user with authentication enabled.
	 */
	public static final String USERNAME = "hibernate.org.couchdb.username";

	/**
	 * The username of the CouchDB database user with authentication enabled.
	 */
	public static final String PASSWORD = "hibernate.org.couchdb.password";

	/**
	 * If true, the database will be created when it doesn't exist. Default to false. The user defined with
	 * {@link #USERNAME} must have the privileges for the creation of a new database. The database will have the name
	 * defined with {@link #DATABASE}.
	 */
	public static final String CREATE_DATABASE = "hibernate.org.couchdb.createdatabase";

	@Override
	public CouchDBGlobalContext getConfigurationBuilder(ConfigurationContext context) {
		return context.createGlobalContext( CouchDBGlobalContextImpl.class, CouchDBEntityContextImpl.class, CouchDBPropertyContextImpl.class );
	}
}
