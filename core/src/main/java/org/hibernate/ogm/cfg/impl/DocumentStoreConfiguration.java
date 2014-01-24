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
package org.hibernate.ogm.cfg.impl;

import java.util.Map;

import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.options.generic.document.AssociationStorageType;
import org.hibernate.ogm.util.impl.configurationreader.ConfigurationPropertyReader;
import org.hibernate.ogm.util.impl.configurationreader.Validators;

/**
 * Provides access to properties common to different document datastores.
 *
 * @author Gunnar Morling
 */
public abstract class DocumentStoreConfiguration {

	/**
	 * The default host to connect to in case the {@link OgmProperties#HOST} property is not set
	 */
	private static final String DEFAULT_HOST = "localhost";

	private static final AssociationStorageType DEFAULT_ASSOCIATION_STORAGE = AssociationStorageType.IN_ENTITY;

	private final String host;
	private final int port;
	private final String databaseName;
	private final String username;
	private final String password;
	private final boolean createDatabase;
	private final AssociationStorageType associationStorage;

	public DocumentStoreConfiguration(Map<?, ?> configurationValues, int defaultPort) {
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationValues );

		this.host = propertyReader.property( OgmProperties.HOST, String.class )
				.withDefault( DEFAULT_HOST )
				.getValue();

		this.port =  propertyReader.property( OgmProperties.PORT, int.class )
				.withDefault( defaultPort )
				.withValidator( Validators.PORT )
				.getValue();

		this.databaseName = propertyReader.property( OgmProperties.DATABASE, String.class )
				.required()
				.getValue();

		this.username = propertyReader.property( OgmProperties.USERNAME, String.class ).getValue();
		this.password = propertyReader.property( OgmProperties.PASSWORD, String.class ).getValue();

		this.createDatabase = propertyReader.property( OgmProperties.CREATE_DATABASE, boolean.class )
				.withDefault( false )
				.getValue();

		associationStorage = propertyReader.property( DocumentStoreProperties.ASSOCIATIONS_STORE, AssociationStorageType.class )
				.withDefault( DEFAULT_ASSOCIATION_STORAGE )
				.getValue();
	}

	/**
	 * @see OgmProperties#HOST
	 * @return The host name of the data store instance
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @see OgmProperties#PORT
	 * @return The port of the data store instance to connect to
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @see OgmProperties#DATABASE
	 * @return the name of the database to connect to
	 */
	public String getDatabaseName() {
		return databaseName;
	}

	/**
	 * @see OgmProperties#USERNAME
	 * @return The user name to be used for connecting with the data store
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @see OgmProperties#PASSWORD
	 * @return The password to be used for connecting with the data store
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @see OgmProperties#CREATE_DATABASE
	 * @return whether to create the database to connect to if not existent or not
	 */
	public boolean isCreateDatabase() {
		return createDatabase;
	}

	/**
	 * @see DocumentStoreProperties#ASSOCIATIONS_STORE
	 * @return where to store associations
	 */
	public AssociationStorageType getAssociationStorageStrategy() {
		return associationStorage;
	}
}
