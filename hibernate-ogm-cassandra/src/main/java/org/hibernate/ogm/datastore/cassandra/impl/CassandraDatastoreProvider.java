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
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.StartStoppable;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.cassandra.CassandraCQL3Dialect;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.Configurable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Cecile Hui Bon Hoa
 * @author Khanh Tuong Maudoux
 */
//FIXME use a connection pool for Cassandra's connections
public class CassandraDatastoreProvider implements DatastoreProvider, StartStoppable, Configurable {
	public static final String CASSANDRA_KEYSPACE = "hibernate.ogm.cassandra.default_keyspace";

	public static final String CASSANDRA_URL = "hibernate.ogm.cassandra.url";

	private Connection connection;
	private String url;
	private String keyspace;
	private String configurationMode;

	private Map<String, Table> metaDataCache = new HashMap<String, Table>( );

	private static final Log log = LoggerFactory.make();

	public CassandraDatastoreProvider() {
		try {
			Class.forName( "org.apache.cassandra.cql.jdbc.CassandraDriver" );
		}
		catch (ClassNotFoundException e) {
			log.unableToLoadDriver("org.apache.cassandra.cql.jdbc.CassandraDriver") ;
		}
	}

	public String getKeyspace() {
		return keyspace;
	}

	@Override
	public void configure(Map configurationValues) {
		if ( configurationValues == null ) {
			throw new IllegalArgumentException( "invalid configuration file: should not be null" );
		}
		keyspace = (String) configurationValues.get( CASSANDRA_KEYSPACE );
		url = (String) configurationValues.get( CASSANDRA_URL );
		configurationMode = (String) configurationValues.get( OgmConfiguration.HIBERNATE_OGM_GENERATE_SCHEMA );

		if ( !OgmConfiguration.GenerateSchemaValue.isValid( configurationMode ) ) {
			log.unexpectedConfiguration(OgmConfiguration.HIBERNATE_OGM_GENERATE_SCHEMA, OgmConfiguration.HIBERNATE_OGM_GENERATE_SCHEMA_DEFAULT.getValue());
			configurationMode = OgmConfiguration.HIBERNATE_OGM_GENERATE_SCHEMA_DEFAULT.getValue();
		}

		if ( keyspace == null ) {
			throw new HibernateException( "Unable to get keyspace value from configuration on key " + CASSANDRA_KEYSPACE );
		}
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return CassandraCQL3Dialect.class;
	}

	public void start() {
		try {
			connection = DriverManager.getConnection( url );
		} catch ( Exception e ) {
			throw new HibernateException( "Unable to connect to Cassandra server " + url, e );
		}
		createKeyspaceIfNeeded();

		executeStatement( "USE " + keyspace + ";", "Unable to switch to keyspace " + keyspace );
	}

	private void createKeyspaceIfNeeded() {
		if ( OgmConfiguration.GenerateSchemaValue.CREATE_DROP.getValue().equals( configurationMode )
				|| OgmConfiguration.GenerateSchemaValue.CREATE.getValue().equals( configurationMode ) ) {
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

	private void createColumnFamilyIfNeeded(String entityName, List<Column> primaryKeyName, List<Column> columns, String error) {
		if ( OgmConfiguration.GenerateSchemaValue.CREATE_DROP.getValue().equals( configurationMode )
				|| OgmConfiguration.GenerateSchemaValue.CREATE.getValue().equals( configurationMode ) ) {

			assert(primaryKeyName != null);

			StringBuilder query = new StringBuilder(  );

			query.append( "CREATE TABLE " )
					.append( entityName )
					.append( " (" );
			for (Column column : columns) {
				String columnType = ((SimpleValue) column.getValue()).getTypeName();
				String innerType = CassandraTypeMapper.INSTANCE.mapper.get( columnType );
				String tmpInnerType = (innerType == null || innerType.equals( "byte" ) || innerType.equals( "bigint" ) || innerType
						.equals( "uuid" ) || innerType.equals( "calendar_date" ) || innerType.equals( "date" ) || innerType
						.equals( "long" ) || innerType.equals( "decimal" )) ? "varchar" : innerType;

				query.append( column.getName() ).append( " " ).append( tmpInnerType ).append( ", " );
			}
			query.append( "PRIMARY KEY (" );
			      String prefix = "";
			for (Column key : primaryKeyName) {
				query.append( prefix );
				prefix = ",";
				query.append( key.getName() );
			}
			query.append( "));" );

			try {
				executeStatement( query.toString(), error );
			}
			catch (HibernateException e) {
				if ( e.getCause() instanceof SQLSyntaxErrorException ) {
					SQLSyntaxErrorException ee = (SQLSyntaxErrorException) e.getCause();
					String message = ee.getMessage();
					if ( message.contains( "already exists in" ) || message.contains( "already existing column" )) {
						//we are good
					}
					else {
						throw e;
					}
				}
				else {
					throw e;
				}
			}
		}
	}

	private void dropKeyspaceIfNeeded() {
		if ( OgmConfiguration.GenerateSchemaValue.CREATE_DROP.getValue().equals( configurationMode ) ) {
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

	public void executeStatement(String statement, String error) {
		try {
			Statement sqlStatement = connection.createStatement();
			sqlStatement.execute( statement );
			sqlStatement.close();
		} catch ( Exception e ) {
			throw new HibernateException( error, e );
		}
	}

	@Override
	public void start(Configuration configuration, SessionFactoryImplementor sessionFactoryImplementor) {
		start();

		Iterator<Table> tables = configuration.getTableMappings();
		while ( tables.hasNext() ) {
			Table table = tables.next();

			this.metaDataCache.put( table.getName(), table );

			List<Column> primaryKeys = new ArrayList<Column>();
			if ( table.isPhysicalTable() ) {
				if ( table.hasPrimaryKey() ) {
					primaryKeys = table.getPrimaryKey().getColumns();
				}
				List<Column> columns = new ArrayList<Column>();
				Iterator<Column> columnsIt = (Iterator<Column>) table.getColumnIterator();

				while ( columnsIt.hasNext() ) {
					columns.add( columnsIt.next() );
				}

				createColumnFamilyIfNeeded(
						table.getName(),
						primaryKeys,
						columns,
						"Unable create table " + table.getName()
				);
			}
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

	public Map<String, Table> getMetaDataCache() {
		return metaDataCache;
	}
}
