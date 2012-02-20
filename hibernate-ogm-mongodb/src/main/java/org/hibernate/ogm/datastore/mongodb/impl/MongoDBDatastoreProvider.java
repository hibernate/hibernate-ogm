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

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.mongodb.MongoDBDialect;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.jndi.spi.JndiService;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 * Provides access to MongoDB system
 * 
 * @author Guillaume Scheibel<guillaume.scheibel@gmail.com>
 */
public class MongoDBDatastoreProvider implements DatastoreProvider, Startable, Stoppable, ServiceRegistryAwareService,
		Configurable {

	private static final Log log = LoggerFactory.make();
	private static final String MONGODB_DATABASE = "hibernate.ogm.mongodb.database";
	private static final String DEFAULT_HOST = "127.0.0.1";
	private static final int DEFAULT_PORT = 27017;
	private static final long serialVersionUID = 1L;
	private Map<?, ?> cfg;
	private boolean isCacheProvided;
	private JndiService jndiService;
	private JtaPlatform jtaPlatform;
	private Mongo mongo;

	public DB getDatabase() {
		Object dbNameObject = this.cfg.get( MONGODB_DATABASE );
		if ( dbNameObject == null ) {
			throw new HibernateException( "The property " + MONGODB_DATABASE
					+ " has not been set into the configuration file" );
		}
		else {
			String dbName = (String) dbNameObject;
			if ( log.isTraceEnabled() ) {
				log.tracef( "Retrieve database %1$s", dbName );
			}
			if ( !this.mongo.getDatabaseNames().contains( dbName ) ) {
				throw new HibernateException( "The database called " + dbName + " doesn't exist, check the "
						+ MONGODB_DATABASE + " property" );
			}
			return this.mongo.getDB( dbName );
		}
	}

	@Override
	public void configure(Map configurationValues) {
		cfg = configurationValues;
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return MongoDBDialect.class;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		jtaPlatform = serviceRegistry.getService( JtaPlatform.class );
		jndiService = serviceRegistry.getService( JndiService.class );
	}

	public void start() {
		if ( !isCacheProvided ) {
			log.info( "Opening connection to MongoDB instance" );
			try {
				Object cfgHost = this.cfg.get( "hibernate.ogm.mongo.host" );
				String host = cfgHost != null ? cfgHost.toString() : DEFAULT_HOST;

				int port = DEFAULT_PORT;
				Object cfgPort = this.cfg.get( "hibernate.ogm.mongo.port" );
				if ( cfgPort != null ) {
					try {
						int temporaryPort = Integer.valueOf( cfgPort.toString() ).intValue();
						if ( temporaryPort < 1 || temporaryPort > 65535 ) {
							throw new HibernateException(
									"The value set for the hibernate.ogm.mongo.port property must be between 1 and 65535" );
						}
						port = temporaryPort;
					}
					catch ( NumberFormatException e ) {
						throw new HibernateException( "The value set for the hibernate.ogm.mongo.port is not a number" );
					}
				}
				if ( log.isTraceEnabled() ) {
					log.tracef( "Opening connection to: %1$s : %1$s", host, String.valueOf( port ) );
				}
				this.mongo = new Mongo( host, port );
				this.isCacheProvided = true;
			}
			catch ( UnknownHostException e ) {
				throw new HibernateException( "The database host cannot be resolved", e );
			}
			catch ( RuntimeException e ) {
				log.unableToInitializeMongoDB( e );
			}
		}
	}

	public void stop() {
		log.info( "Closing connection to MongoDB instance" );
		this.mongo.close();
	}
}
