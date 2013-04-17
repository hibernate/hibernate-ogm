/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb;

/**
 * Configuration options of the MongoDB GridDialect
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public interface Environment {

	/**
	 * The MongoDB Database name to connect to.
	 */
	public static final String MONGODB_DATABASE = "hibernate.ogm.mongodb.database";

	/**
	 * The port of the MongoDB instance.
	 */
	public static final String MONGODB_PORT = "hibernate.ogm.mongodb.port";

	/**
	 * Run the driver in safe mode (use WriteConcern.SAFE for all operations)
	 */
	public static final String MONGODB_SAFE = "hibernate.ogm.mongodb.safe";

	/**
	 * The hostname of the MongoDB instance.
	 */
	public static final String MONGODB_HOST = "hibernate.ogm.mongodb.host";

	/**
	 * The username of the MongoDB admin database with authentication enabled.
	 */
	public static final String MONGODB_USERNAME = "hibernate.ogm.mongodb.username";

	/**
	 * The password of the MongoDB admin database with authentication enabled.
	 */
	public static final String MONGODB_PASSWORD = "hibernate.ogm.mongodb.password";

	/**
	 * The timeout used at the connection to the MongoDB instance.
	 * This value is set in milliseconds.
	 */
	public static final String MONGODB_TIMEOUT = "hibernate.ogm.mongodb.connection_timeout";

	/**
	 * The default host used to connect to MongoDB: if the {@link #MONGODB_HOST}
	 * property is not set, we'll attempt to connect to localhost.
	 */
	public static final String MONGODB_DEFAULT_HOST = "127.0.0.1";

	/**
	* The default port used to connect to MongoDB: if the {@link #MONGODB_PORT}
	* property is not set, we'll try this port.
	*/
	public static final int MONGODB_DEFAULT_PORT = 27017;

	/**
	 * The default value used to configure the safe mode {@link #MONGODB_SAFE}
	 */
	public static final boolean MONGODB_DEFAULT_SAFE = true;

	/**
	 * Where to store associations.
	 */
	public static final String MONGODB_ASSOCIATIONS_STORE = "hibernate.ogm.mongodb.associations.store";
	public static final String MONGODB_DEFAULT_ASSOCIATION_STORE = "Associations";

	/**
	 * The default value used to set the timeout during the connection to the MongoDB instance (@link #MONGODB_TIMEOUT)
	 * This value is set in milliseconds
	 */
	public static final int MONGODB_DEFAULT_TIMEOUT = 5000;
}
