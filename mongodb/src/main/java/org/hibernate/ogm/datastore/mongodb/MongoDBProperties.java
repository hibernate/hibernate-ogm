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
package org.hibernate.ogm.datastore.mongodb;

/**
 * Properties for configuring the MongoDB datastore via {@code persistence.xml} or
 * {@link org.hibernate.ogm.cfg.OgmConfiguration}.
 *
 * @author Gunnar Morling
 */
public final class MongoDBProperties {

	/**
	 * Configuration property for defining the acknowledgement of write operations
	 *
	 * @see com.mongodb.WriteConcern
	 */
	public static final String WRITE_CONCERN = "hibernate.ogm.mongodb.writeconcern";

	/**
	 * The timeout used at the connection to the MongoDB instance. This value is set in milliseconds.
	 */
	public static final String TIMEOUT = "hibernate.ogm.mongodb.connection_timeout";

	/**
	 * Where to store associations.
	 */
	public static final String ASSOCIATIONS_STORE = "hibernate.ogm.mongodb.associations.store";

	private MongoDBProperties() {
	}
}
