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
import org.hibernate.mapping.Table;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.StartStoppable;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.cassandra.CassandraCQL3Dialect;
import org.hibernate.ogm.persister.OgmEntityPersister;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.Configurable;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.AlreadyExistsException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Cecile Hui Bon Hoa
 * @author Khanh Tuong Maudoux
 */
//FIXME use a connection pool for Cassandra's connections
public class CassandraDatastoreProvider implements DatastoreProvider, StartStoppable, Configurable {
	public static final String CASSANDRA_KEYSPACE = "hibernate.ogm.cassandra.default_keyspace";

	public static final String CASSANDRA_URL = "hibernate.ogm.cassandra.url";

	private String url;
	private String keyspace;
	private String configurationMode;

	private Cluster cluster;
	private Session session;

	Map<String, Map<String, String>> entitiesMetaData = new HashMap<String, Map<String, String>>(  );

	private static final Log log = LoggerFactory.make();

	public CassandraDatastoreProvider() {
	}

	public Session getSession() {
		return session;
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
			log.unexpectedConfiguration(
					OgmConfiguration.HIBERNATE_OGM_GENERATE_SCHEMA,
					OgmConfiguration.HIBERNATE_OGM_GENERATE_SCHEMA_DEFAULT.getValue()
			);
			configurationMode = OgmConfiguration.HIBERNATE_OGM_GENERATE_SCHEMA_DEFAULT.getValue();
		}

		if ( keyspace == null ) {
			log.unableToGetKeyspace( CASSANDRA_KEYSPACE );
			throw log.unableToGetKeyspace( CASSANDRA_KEYSPACE );
		}
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return CassandraCQL3Dialect.class;
	}

	public void start() {
		try {
			cluster = new Cluster.Builder().addContactPoints( url ).build();
			session = cluster.connect();
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to connect to Cassandra server " + url, e );
		}
		createKeyspaceIfNeeded();

		try {
			session.execute( "USE " + keyspace );
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to use keyspace " + keyspace, e );
		}
	}

	private void createKeyspaceIfNeeded() {
		if ( OgmConfiguration.GenerateSchemaValue.CREATE_DROP.getValue().equals( configurationMode )
				|| OgmConfiguration.GenerateSchemaValue.CREATE.getValue().equals( configurationMode ) ) {
			try {
				String CREATE_KEYSPACE_SIMPLE_FORMAT = "CREATE KEYSPACE %s WITH replication = { 'class' : 'SimpleStrategy', 'replication_factor' : %d }";
				session.execute( String.format( CREATE_KEYSPACE_SIMPLE_FORMAT, keyspace, 1 ) );
			}
			catch (AlreadyExistsException e) {
				log.info( "unable to create the keyspace {}", keyspace, e );
			}
			catch (NoHostAvailableException e) {
				throw new HibernateException( "Unable create the keyspace " + keyspace, e );
			}
		}
	}

	private void createColumnFamilyIfNeeded(
			String entityName,
			List<Column> primaryKeyName,
			List<Column> columns,
			Map<String, String> columnMetadata) {
		if ( OgmConfiguration.GenerateSchemaValue.CREATE_DROP.getValue().equals( configurationMode )
				|| OgmConfiguration.GenerateSchemaValue.CREATE.getValue().equals( configurationMode ) ) {

			assert (primaryKeyName != null);

			StringBuilder query = new StringBuilder();

			query.append( "CREATE TABLE " )
					.append( entityName )
					.append( " (" );
			for ( Column column : columns ) {
				//TODO : hack for unknown types : what should we do?
				String columnName = column.getName();
				String columnType = columnMetadata.get( columnName );
				if (!columnType.contains( "." )) {
					//for standard usecase ie. nom embedded
					if (!columnName.contains( "." )) {
						query.append( columnName )
								.append( " " )
								.append( columnType )
								.append( ", " );
					} else {
						//TODO : hack for Embeddable usecase. For now, can handle only one level
						if ( columnType.contains( "component[" )) {
							//ex: component[city,country,street1,street2,zipCode]

							//NOTHING TO DO : already handle : embedded columns are already return
							columnType = "text";
						}
							//TODO : the character . is forbidden
							columnName = columnName.replaceAll( "\\.", "_" );
							query.append( columnName )
									.append( " " )
									.append( columnType )
									.append( ", " );
						}
				} else {                  //TODO : if the character "." is found, it should an association?
					query.append( columnName )
							.append( " " )
							.append( "text" )
							.append( ", " );
				}
			}

			//TODO: hack: if a columnFamily has just a single column, add a fake column called dtype. What should we do?
			//TODO: cassandra can not insert just the pk and need at least one column: http://cassandra.apache.org/doc/cql/CQL.html#INSERT
			if (columns.size() == 1) {
				query.append( " dtype text, " );
			}

			query.append( "PRIMARY KEY (" );
			String prefix = "";
			for ( Column key : primaryKeyName ) {
				query.append( prefix );
				prefix = ",";
				query.append( key.getName() );
			}
			query.append( " ))" );
			try {
				session.execute( query.toString() );
			}
			catch (AlreadyExistsException e) {
				log.info( "unable to create cassandra table {} into keyspace {}", new String[] {entityName, keyspace}, e );
			}
			catch (NoHostAvailableException e) {
				throw new HibernateException( "Unable to create cassandra table", e );
			}
		}
	}

	private void dropKeyspaceIfNeeded() {
		if ( OgmConfiguration.GenerateSchemaValue.CREATE_DROP.getValue().equals( configurationMode ) ) {
			forceDropSchema();
		}
	}

	public void forceDropSchema() {
		try {
			session.execute( "DROP KEYSPACE " + keyspace );
		}
		catch (Exception e) {
			throw new HibernateException( "Unable to drop the keyspace " + keyspace, e );
		}
	}


	@Override
	public void start(Configuration configuration, SessionFactoryImplementor sessionFactoryImplementor) {
		start();

		Set<String> entitiesPersisterName = sessionFactoryImplementor.getEntityPersisters().keySet();

		Set<Table> tables = new HashSet<Table>(  );
		Iterator<Table> tablesIt = configuration.getTableMappings();

		while (tablesIt.hasNext()) {
			tables.add( tablesIt.next() );
		}

		for ( String entityPersisterName : entitiesPersisterName ) {
			Map<String, String> columnMetadata = new HashMap<String, String>();

			Table table = configuration.getClassMapping( entityPersisterName ).getTable();
			String entityName = table.getName();

			// in case of inheritance, entityPersisterName does not have all tables
			if (!tables.contains( table )) {
				break;
			}

			tables.remove( table );

			createColumnFamilyFromEntityPersister(
					sessionFactoryImplementor,
					entityPersisterName,
					columnMetadata,
					table,
					entityName
			);
		}
		if (!tables.isEmpty()) {
			for (Table table : tables) {
				createColumnFamilyNotHandleByEntityPersister( table );


				Iterator<Column> columnsIt = table.getColumnIterator();
				while (columnsIt.hasNext()) {
					Column column = columnsIt.next();

					if (!entitiesMetaData.get(table.getName()).containsKey( column.getName() )) {
						break;
					}

					createIndexForTableAndColumn( table, column );
				}
			}
		}
	}

	private void createColumnFamilyNotHandleByEntityPersister(Table table) {
		List<Column> columns = table.getPrimaryKey().getColumns();
		Column keyColumn = new Column( "id_" );

		Map<String, String> columnMetadata = new HashMap<String, String>();

		for (Column column : columns) {
			columnMetadata.put( column.getName(), "text" );
		}
		columns.add( keyColumn );

		List<Column> keysColumn = new ArrayList<Column>(  );
		keysColumn.add( keyColumn );

		columnMetadata.put( "id_", "text" );
		entitiesMetaData.put( table.getName(), columnMetadata );

		createColumnFamilyIfNeeded(
				table.getName(),
				keysColumn,
				columns,
				entitiesMetaData.get( table.getName() )
		);
	}

	private void createColumnFamilyFromEntityPersister(
			SessionFactoryImplementor sessionFactoryImplementor,
			String entityPersisterName, Map<String, String> columnMetadata, Table table, String entityName) {
		if ( table.isPhysicalTable() ) {
			if ( table.hasPrimaryKey() ) {
				List<Column> primaryKeys = table.getPrimaryKey().getColumns();
				OgmEntityPersister entityPersister = (OgmEntityPersister) (sessionFactoryImplementor.getEntityPersister(
						entityPersisterName
				));
				//TODO only for single pk
				columnMetadata.put(
						primaryKeys.get( 0 ).getName(),
						entityPersister.getGridIdentifierType().getName()
				);

				List<Column> columns = new ArrayList<Column>();
				Iterator<Column> columnsIt = (Iterator<Column>) table.getColumnIterator();

				while ( columnsIt.hasNext() ) {
					columns.add( columnsIt.next() );
				}

				for ( Column column : columns ) {
					if ( !column.getName( ).equals( primaryKeys.get( 0 ).getName())  ) {
						columnMetadata.put( column.getName(), getTypeFromEntityPersisterTypeName(entityPersister, column.getName()) );
					}
				}

				entitiesMetaData.put( entityName, columnMetadata );

				createColumnFamilyIfNeeded(
						entityName,
						primaryKeys,
						columns,
						columnMetadata
				);
			}
		}
	}

	private void createIndexForTableAndColumn(Table table, Column column) {
		StringBuilder query = new StringBuilder();

		query.append( "CREATE INDEX ON " )
				.append( table.getName() )
				.append( " (" )
				.append( column.getName() )
				.append( " )" );
		try {
			session.execute( query.toString() );
		}
		catch (AlreadyExistsException e) {
			log.info( "unable to create index {} into table {}", new String[] {column.getName(), table.getName()}, e );
		}
		catch (NoHostAvailableException e) {
			throw new HibernateException( "Unable to create index", e );
		}
	}


	private String getTypeFromEntityPersisterTypeName(OgmEntityPersister entityPersister, String typeName) {
		int i = 0;
		GridType[] gridTypes = entityPersister.getGridPropertyTypes();

		while (i < gridTypes.length) {
			//TODO only for single pk
			if (typeName.equalsIgnoreCase( entityPersister.getPropertyColumnNames( i )[0])) {
				return gridTypes[i].getName();
			}
			i++;
		}
		//TODO : hack for default type. What should we do?
		return "text";
	}

	@Override
	public void stop() {
		if ( session != null ) {
			dropKeyspaceIfNeeded();
			session.shutdown();
		}
	}
}
