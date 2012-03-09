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
package org.hibernate.ogm.datastore.cassandra.impl;

import org.hibernate.HibernateException;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.cassandra.CassandraCQL2Dialect;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.search.util.impl.ClassLoaderHelper;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.Map;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Cecile Hui Bon Hoa
 * @author Khanh Tuong Maudoux
 */
//FIXME use a connection pool for Cassandra's connections
public class CassandraDatastoreProvider implements DatastoreProvider, Startable, Stoppable,
		ServiceRegistryAwareService, Configurable {
	public static final String CASSANDRA_KEYSPACE = "hibernate.ogm.cassandra.default_keyspace";

	public static final String CASSANDRA_URL = "hibernate.ogm.cassandra.url";

	public static final String CASSANDRA_HBM2DDL_AUTO = AvailableSettings.HBM2DDL_AUTO;
	public static final String CASSANDRA_HBM2DDL_AUTO_DEFAULT = "validate";

	private Connection connection;
	private String url;
	private String keyspace;
	private String configurationMode;

	private static final Log log = LoggerFactory.make();

	public CassandraDatastoreProvider() {
		ClassLoaderHelper.classForName( "org.apache.cassandra.cql.jdbc.CassandraDriver", CassandraDatastoreProvider.class, "Cassandra Driver" );
	}

	@Override
	public void configure(Map configurationValues) {
		if ( configurationValues == null ) {
			throw new IllegalArgumentException( "invalid configuration file: should not be null" );
		}
		keyspace = (String) configurationValues.get( CASSANDRA_KEYSPACE );
		url = (String) configurationValues.get( CASSANDRA_URL );
		configurationMode = (String) configurationValues.get( CASSANDRA_HBM2DDL_AUTO );

		if ( configurationMode == null ) {
			configurationMode = CASSANDRA_HBM2DDL_AUTO_DEFAULT;
		}

		if ( keyspace == null ) {
			throw new HibernateException( "Unable to get keyspace value from configuration on key " + CASSANDRA_KEYSPACE );
		}
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return CassandraCQL2Dialect.class;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
	}

	@Override
	public void start() {
		try {
			connection = DriverManager.getConnection( url );
		} catch ( Exception e ) {
			throw new HibernateException( "Unable to connect to Cassandra server " + url, e );
		}

		createKeyspaceIfNeeded();

		executeStatement( "USE " + keyspace + ";", "Unable to switch to keyspace " + keyspace );

		//FIXME today we put everything in the same table because Emmanuel does not know how to get the SessionFactory in a service...
		//FIXME In the end some kind of "lazy" table creation probably makes sense with and ping pong between the datastore and the dialect
		StringBuilder statement = new StringBuilder()
				.append( "CREATE TABLE " )
				.append( "GenericTable (" )
				.append( "key blob PRIMARY KEY);" );
		//FIXME find a way to bind the key type to the cassandra type: blob sucks from a user PoV
		try {
			executeStatement( statement.toString(), "Unable create table GenericTable" );
		} catch ( HibernateException e ) {
			if ( e.getCause() instanceof SQLSyntaxErrorException ) {
				SQLSyntaxErrorException ee = (SQLSyntaxErrorException) e.getCause();
				String message = ee.getMessage();
				if ( message.contains( "already exists in" ) ) {
					//we are good
				} else {
					throw e;
				}
			} else {
				throw e;
			}
		}
	}

	private void createKeyspaceIfNeeded() {
		if ( "create-drop".equals( configurationMode )
				|| "create".equals( configurationMode ) ) {
			try {
				StringBuilder statement = new StringBuilder()
						.append( "CREATE KEYSPACE " )
						.append( keyspace )
						.append( " WITH strategy_class = 'SimpleStrategy'" )
						.append( " AND strategy_options:replication_factor = 1;" );
				Statement sqlStatement = connection.createStatement();
				sqlStatement.execute( statement.toString() );
				sqlStatement.close();
			} catch ( SQLSyntaxErrorException e ) {
				if ( !e.getMessage().startsWith( "Keyspace names must be case-insensitively unique" ) ) {
					throw new HibernateException( "Unable to create the keyspace " + keyspace, e );
				}
				//else the keyspace is already created
			} catch ( Exception e ) {
				throw new HibernateException( "Unable create the keyspace " + keyspace, e );
			}
		}
	}

	private void dropKeyspaceIfNeeded() {
		if ( "create-drop".equals( configurationMode ) ) {
			try {
				StringBuilder statement = new StringBuilder()
						.append( "DROP KEYSPACE " )
						.append( keyspace )
						.append( ";" );
				Statement sqlStatement = connection.createStatement();
				sqlStatement.execute( statement.toString() );
				sqlStatement.close();
			} catch ( SQLException e ) {
				if ( !e.getMessage().startsWith( "Keyspace names must be case-insensitively unique" ) ) {
					throw new HibernateException( "Unable to drop the keyspace " + keyspace, e );
				}
			}
		}
	}

	private void executeStatement(String statement, String error) {
		try {
			Statement sqlStatement = connection.createStatement();
			sqlStatement.execute( statement );
			sqlStatement.close();
		} catch ( Exception e ) {
			throw new HibernateException( error, e );
		}
	}

	@Override
	public void stop() {
		if ( connection != null ) {
			try {
				dropKeyspaceIfNeeded();
				connection.close();
			} catch ( SQLException e ) {
				//FIXME log a warning
			}
		}
	}

	public Connection getConnection() {
		return connection;
	}
}
