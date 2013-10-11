/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb.impl.configuration;

import java.util.Locale;
import java.util.Map;

import com.mongodb.MongoClientOptions;
import com.mongodb.WriteConcern;

import org.hibernate.ogm.datastore.mongodb.AssociationStorageType;
import org.hibernate.ogm.logging.mongodb.impl.Log;
import org.hibernate.ogm.logging.mongodb.impl.LoggerFactory;

/**
 * Configuration for {@link org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider}.
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class MongoDBConfiguration {

	private static final Log log = LoggerFactory.getLogger();

	private String host;
	private int port;
	private AssociationStorageType associationStorage;
	private String databaseName;
	private String username;
	private String password;
	private int timeout;
	private WriteConcern writeConcern;

	/**
	 * @see Environment#MONGODB_HOST
	 * @return The hostname of the MongoDB instance
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @see Environment#MONGODB_PASSWORD
	 * @return The password of the MongoDB admin database with authentication enabled
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @see Environment#MONGODB_USERNAME
	 * @return The username of the MongoDB admin database with authentication enabled
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @see Environment#MONGODB_DATABASE
	 * @return the MongoDB Database name to connect to
	 */
	public String getDatabaseName() {
		return databaseName;
	}

	/**
	 * @see Environment#MONGODB_ASSOCIATIONS_STORE
	 * @return where to store associations
	 */
	public AssociationStorageType getAssociationStorage() {
		return associationStorage;
	}

	/**
	 * @see Environment#MONGODB_PORT
	 * @return The port of the MongoDB instance
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Initialize the internal values from the given {@link Map}.
	 *
	 * @see Environment
	 * @param configurationMap
	 *            The values to use as configuration
	 */
	public void initialize(Map configurationMap) {
		this.host = this.buildHost( configurationMap );
		this.port = this.buildPort( configurationMap );
		this.timeout = this.buildTimeout( configurationMap );
		log.connectingToMongo( host, port, timeout );

		this.associationStorage = this.buildAssociationStorage( configurationMap );
		this.writeConcern = this.buildWriteConcern( configurationMap );
		this.databaseName = this.buildDatabase( configurationMap );
		this.username = this.buildUsername( configurationMap );
		this.password = this.buildPassword( configurationMap );
	}

	private String buildHost(Map<?, ?> cfg) {
		Object cfgHost = cfg.get( Environment.MONGODB_HOST );
		return cfgHost != null ? cfgHost.toString() : Environment.MONGODB_DEFAULT_HOST;
	}

	private int buildPort(Map<?, ?> cfg) {
		Object cfgPort = cfg.get( Environment.MONGODB_PORT );
		if ( cfgPort != null ) {
			try {
				int temporaryPort = Integer.valueOf( cfgPort.toString() );
				if ( temporaryPort < 1 || temporaryPort > 65535 ) {
					throw log.mongoPortIllegalValue( cfgPort.toString() );
				}
				return temporaryPort;
			}
			catch ( NumberFormatException e ) {
				throw log.mongoPortIllegalValue( cfgPort.toString() );
			}
		}
		else {
			return Environment.MONGODB_DEFAULT_PORT;
		}
	}

	private AssociationStorageType buildAssociationStorage(Map<?, ?> cfg) {
		String assocStoreString = (String) cfg.get( Environment.MONGODB_ASSOCIATIONS_STORE );
		if ( assocStoreString == null ) {
			//default value
			return AssociationStorageType.IN_ENTITY;
		}
		else {
			try {
				return AssociationStorageType.valueOf( assocStoreString.toUpperCase( Locale.ENGLISH ) );
			}
			catch ( IllegalArgumentException e ) {
				throw log.unknownAssociationStorageStrategy( assocStoreString, AssociationStorageType.class );
			}
		}
	}

	private WriteConcern buildWriteConcern(Map<?, ?> cfg) {
		Object cfgWriteConcern = cfg.get( Environment.MONGODB_WRITE_CONCERN );
		WriteConcern writeConcern = Environment.MONGODB_DEFAULT_WRITE_CONCERN;
		String wcLogMessage = "ACKNOWLEDGED";
		if ( cfgWriteConcern != null ) {
			final String confWC = cfgWriteConcern.toString();
			writeConcern = WriteConcern.valueOf( confWC );

			if ( writeConcern == null ) {
				writeConcern = Environment.MONGODB_DEFAULT_WRITE_CONCERN;
				wcLogMessage = "ACKNOWLEDGED";
			}
			else {
				wcLogMessage = confWC;
			}
		}
		// using a custom string representation because neither toString() nor getWString() return a user-friendly message
		log.useWriteConcern( wcLogMessage );
		return writeConcern;
	}

	private int buildTimeout(Map<?, ?> cfg) {
		Object cfgTimeout = cfg.get( Environment.MONGODB_TIMEOUT );
		if ( cfgTimeout != null ) {
			try {
				int temporaryTimeout = Integer.valueOf( cfgTimeout.toString() );
				if ( temporaryTimeout < 0 ) {
					throw log.mongoDBTimeOutIllegalValue( cfgTimeout.toString() );
				}
				return temporaryTimeout;
			}
			catch ( NumberFormatException e ) {
				throw log.mongoDBTimeOutIllegalValue( cfgTimeout.toString() );
			}
		}
		else {
			return Environment.MONGODB_DEFAULT_TIMEOUT;
		}
	}

	private String buildDatabase(Map<?, ?> cfg) {
		Object dbNameObject = cfg.get( Environment.MONGODB_DATABASE );
		if ( dbNameObject == null ) {
			throw log.mongoDbNameMissing();
		}
		String dbName = (String) dbNameObject;
		log.connectingToMongoDatabase( dbName );
		return dbName;
	}

	private String buildUsername(Map<?, ?> cfg) {
		return (String) cfg.get( Environment.MONGODB_USERNAME );
	}

	private String buildPassword(Map<?, ?> cfg) {
		Object passwordObject = cfg.get( Environment.MONGODB_PASSWORD );
		return passwordObject != null ? passwordObject.toString() : "";
	}

	/**
	 * Create a {@link MongoClientOptions} using the {@link MongoDBConfiguration}.
	 *
	 * @return the {@link MongoClientOptions} corresponding to the {@link MongoDBConfiguration}
	 */
	public MongoClientOptions buildOptions() {
		MongoClientOptions.Builder optionsBuilder = new MongoClientOptions.Builder();
		optionsBuilder.connectTimeout( timeout );
		optionsBuilder.writeConcern( writeConcern );

		return optionsBuilder.build();
	}
}
