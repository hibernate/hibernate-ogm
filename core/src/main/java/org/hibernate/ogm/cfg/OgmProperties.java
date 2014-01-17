/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.cfg;

/**
 * Common properties for configuring NoSql datastores via {@code persistence.xml} or
 * {@link org.hibernate.ogm.cfg.OgmConfiguration}.
 * <p>
 * Note that not all properties are supported by all datastores; refer to the documentation of the specific dialect to
 * find out the supported configuration properties.
 *
 * @author Gunnar Morling
 */
public final class OgmProperties {

	/**
	 * Property for setting the datastore provider. Can take the following values:
	 * <ul>
	 * <li>a {@code DatastoreProvider} instance</li>
	 * <li>a {@code DatastoreProvider} class</li>
	 * <li>a string representing the {@code DatastoreProvider} class</li>
	 * <li>a string representing one of the datastore provider shortcuts</li>
	 * </ul>
	 * If the property is not set, Infinispan is used by default.
	 */
	public static final String DATASTORE_PROVIDER = "hibernate.ogm.datastore.provider";

	/**
	 * Property for setting the host name to connect to. Accepts {@code String}.
	 */
	public static final String HOST = "hibernate.ogm.datastore.host";

	/**
	 * Property for setting the port number of the database to connect to. Accepts {@code int}.
	 */
	public static final String PORT = "hibernate.ogm.datastore.port";

	/**
	 * Property for setting the name of the database to connect to. Accepts {@code String}.
	 */
	public static final String DATABASE = "hibernate.ogm.datastore.database";

	/**
	 * Property for setting the user name to connect with. Accepts {@code String}.
	 */
	public static final String USERNAME = "hibernate.ogm.datastore.username";

	/**
	 * Property for setting the password to connect with. Accepts {@code String}.
	 */
	public static final String PASSWORD = "hibernate.ogm.datastore.password";

	private OgmProperties() {
	}
}
