/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import com.datastax.driver.core.exceptions.DriverException;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

import org.hibernate.mapping.Table;

import org.hibernate.ogm.datastore.cassandra.CassandraDialect;
import org.hibernate.ogm.datastore.cassandra.impl.configuration.CassandraConfiguration;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

import org.hibernate.persister.collection.CollectionPersister;

import org.hibernate.ogm.datastore.cassandra.logging.impl.Log;
import org.hibernate.ogm.datastore.cassandra.logging.impl.LoggerFactory;

import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * Datastore service layered on Cassandra's java-driver i.e. CQL3 native transport.
 *
 * @author Jonathan Halliday
 */
public class CassandraDatastoreProvider extends BaseDatastoreProvider
		implements Startable, Stoppable, ServiceRegistryAwareService, Configurable {

	private static final Log log = LoggerFactory.getLogger();

	private ServiceRegistryImplementor serviceRegistry;

	private CassandraConfiguration config;

	private Cluster cluster;
	private Session session;
	private CassandraSequenceHandler sequenceHandler;

	private final Set<String> entityTableNames = new HashSet<String>();

	private final Map<String, Table> metaDataCache = new HashMap<String, Table>();

	public void setTableMetadata(String name, Table table) {
		metaDataCache.put( name, table );
	}


	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return CassandraSchemaDefiner.class;
	}

	public CassandraSequenceHandler getSequenceHandler() {
		return sequenceHandler;
	}

	@Override
	public void configure(Map configurationValues) {
		OptionsService optionsService = serviceRegistry.getService( OptionsService.class );
		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader(
				configurationValues,
				classLoaderService
		);
		config = new CassandraConfiguration( propertyReader, optionsService.context().getGlobalOptions() );
	}

	public Session getSession() {
		return session;
	}

	public Map<String, Table> getMetaDataCache() {
		return metaDataCache;
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return CassandraDialect.class;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void start() {
		if ( cluster == null ) {
			try {
				log.connectingToCassandra( config.getHost(), config.getPort() );

				cluster = new Cluster.Builder()
						.addContactPoint( config.getHost() )
						.withPort( config.getPort() )
						.build();

				Session bootstrapSession = cluster.connect();
				bootstrapSession.execute(
						"CREATE KEYSPACE IF NOT EXISTS " + config.getDatabaseName() +
								" WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 }"
				);
				bootstrapSession.close();

				session = cluster.connect( config.getDatabaseName() );

				sequenceHandler = new CassandraSequenceHandler(this);
			}
			catch (RuntimeException e) {
				throw log.unableToInitializeCassandra( e );
			}
		}
	}

	@Override
	public void stop() {
		log.disconnectingFromCassandra();
		session.close();
		session = null;
		cluster.close();
		cluster = null;
		sequenceHandler = null;
	}

	public void removeKeyspace() {
		Session bootstrapSession = cluster.connect();
		bootstrapSession.execute( "DROP KEYSPACE " + config.getDatabaseName() );
		bootstrapSession.close();
	}

	// test harness
	public long countAllEntities() {
		long count = 0;
		for ( Table table : metaDataCache.values() ) {
			if ( table.getIdentifierValue() != null ) {
				StringBuilder query = new StringBuilder( "SELECT COUNT(*) FROM \"" );
				query.append( table.getName() );
				query.append( "\"" );
				Row row = session.execute( query.toString() ).one();
				count += row.getLong( 0 );
			}
		}
		return count;
	}

	// test harness
	public long countAllAssociations(Collection<CollectionPersister> collectionPersisters) {
		long count = 0;
		for ( CollectionPersister collectionPersister : collectionPersisters ) {
			AssociationKeyMetadata associationKeyMetadata = ((OgmCollectionPersister) collectionPersister).getAssociationKeyMetadata();
			StringBuilder query = new StringBuilder( "SELECT \"" );
			query.append( associationKeyMetadata.getColumnNames()[0] );
			query.append( "\" FROM \"" );
			query.append( associationKeyMetadata.getTable() );
			query.append( "\"" );
			ResultSet resultSet = session.execute( query.toString() );
			// no GROUP BY in CQL, so we do it the hard way...
			HashSet<String> uniqs = new HashSet<String>();
			for ( Row row : resultSet ) {
				uniqs.add( row.getBytesUnsafe( 0 ).toString() );
			}
			count += uniqs.size();
		}

		return count;
	}

	public void createSecondaryIndexIfNeeded(String entityName, String columnName) {

		// quoting index names: https://issues.apache.org/jira/browse/CASSANDRA-8393 (fixed in 2.1.3)

		StringBuilder query = new StringBuilder();
		query.append( "CREATE INDEX IF NOT EXISTS " );
		query.append( "\"" );
		query.append( entityName );
		query.append( "_" );
		// hibernate allows '.' in some cases e.g. embeddedId fields, but c* doesn't like them in identifiers.
		String safeColumnName = columnName.replace( '.', '_' );
		query.append( safeColumnName );
		query.append( "\"" );
		query.append( " ON " );
		query.append( "\"" );
		query.append( entityName );
		query.append( "\"" );
		query.append( " (" );
		query.append( "\"" );
		query.append( columnName );
		query.append( "\"" );
		query.append( ")" );

		try {
			session.execute( query.toString() );
		}
		catch (DriverException e) {
			log.failedToCreateIndex( entityName, e );
		}
	}

	public void createColumnFamilyIfNeeded(
			String entityName, List<String> primaryKeyName,
			List<String> columnNames, List<String> columnTypes) {

		assert (primaryKeyName != null);

		StringBuilder query = new StringBuilder();

		query.append( "CREATE TABLE IF NOT EXISTS " )
				.append( "\"" )
				.append( entityName )
				.append( "\"" )
				.append( " (" );

		for ( int i = 0; i < columnNames.size(); i++ ) {
			String columnType = columnTypes.get( i );
			query.append( "\"" );
			query.append( columnNames.get( i ) );
			query.append( "\"" );
			query.append( " " ).append( columnType ).append( ", " );
		}
		query.append( "PRIMARY KEY (" );
		String prefix = "";
		for ( String key : primaryKeyName ) {
			query.append( prefix );
			prefix = ",";

			query.append( "\"" );
			query.append( key );
			query.append( "\"" );
		}
		query.append( "));" );

		try {
			session.execute( query.toString() );
			entityTableNames.add( entityName );
		}
		catch (DriverException e) {
			log.failedToCreateTable( entityName, e );
		}
	}
}
