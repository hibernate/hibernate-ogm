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
package org.hibernate.ogm.datastore.mongodb.impl;

import java.net.UnknownHostException;
import java.util.Map;

import org.hibernate.ogm.datastore.mongodb.Environment;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.mongodb.MongoDBDialect;
import org.hibernate.ogm.logging.mongodb.impl.Log;
import org.hibernate.ogm.logging.mongodb.impl.LoggerFactory;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 * Provides access to MongoDB system
 * 
 * @author Guillaume Scheibel<guillaume.scheibel@gmail.com>
 */
public class MongoDBDatastoreProvider implements DatastoreProvider, Startable, Stoppable, Configurable {

	private static final Log log = LoggerFactory.getLogger();

	private Map<?, ?> cfg;
	private boolean isCacheStarted;
	private Mongo mongo;
	private DB mongoDb;

	@Override
	public void configure(Map configurationValues) {
		cfg = configurationValues;
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return MongoDBDialect.class;
	}

	@Override
	public void start() {
		if ( !isCacheStarted ) {
			Object cfgHost = this.cfg.get( Environment.MONGODB_HOST );
			String host = cfgHost != null ? cfgHost.toString() : Environment.MONGODB_DEFAULT_HOST;
			try {
				final int port;
				Object cfgPort = this.cfg.get( Environment.MONGODB_PORT );
				if ( cfgPort != null ) {
					try {
						int temporaryPort = Integer.valueOf( cfgPort.toString() ).intValue();
						if ( temporaryPort < 1 || temporaryPort > 65535 ) {
							throw log.mongoPortIllegalValue();
						}
						port = temporaryPort;
					}
					catch ( NumberFormatException e ) {
						throw log.mongoPortIllegalValue();
					}
				}
				else {
					port = Environment.MONGODB_DEFAULT_PORT;
				}
				log.connectingToMongo( host, port );
				this.mongo = new Mongo( host, port );
				this.isCacheStarted = true;
			}
			catch ( UnknownHostException e ) {
				throw log.mongoOnUnknownHost( host );
			}
			catch ( RuntimeException e ) {
				throw log.unableToInitializeMongoDB( e );
			}
			mongoDb = extractDatabase();
		}
	}

	@Override
	public void stop() {
		log.disconnectingFromMongo();
		this.mongo.close();
	}

	public DB getDatabase() {
		return mongoDb;
	}


	private DB extractDatabase() {
		Object dbNameObject = this.cfg.get( Environment.MONGODB_DATABASE );
		if ( dbNameObject == null ) {
			throw log.mongoDbNameMissing();
		}
		String dbName = (String) dbNameObject;
		log.connectingToMongoDatabase( dbName );
		if ( !this.mongo.getDatabaseNames().contains( dbName ) ) {
			log.creatingDatabase( dbName );
		}
		return this.mongo.getDB( dbName );
	}

}
