/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.impl;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.spi.Hosts;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.RemoteNeo4jDialect;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jBasedQueryParserService;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.remote.transaction.impl.RemoteTransactionCoordinatorBuilder;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * @author Davide D'Alto
 */
public class RemoteNeo4jDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final int DEFAULT_SEQUENCE_QUERY_CACHE_MAX_SIZE = 128;

	private static final Log logger = LoggerFactory.getLogger();

	private Integer sequenceCacheMaxSize;

	private Neo4jConfiguration configuration;

	private Neo4jClient remoteNeo4j;

	private RemoteSequenceGenerator sequenceGenerator;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return RemoteNeo4jDialect.class;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return Neo4jBasedQueryParserService.class;
	}

	@Override
	public void configure(Map configurationValues) {
		configuration = new Neo4jConfiguration( new ConfigurationPropertyReader( configurationValues ) );
		sequenceCacheMaxSize = new ConfigurationPropertyReader( configurationValues )
				.property( Neo4jProperties.SEQUENCE_QUERY_CACHE_MAX_SIZE, int.class )
				.withDefault( DEFAULT_SEQUENCE_QUERY_CACHE_MAX_SIZE )
				.getValue();
	}

	@Override
	public void stop() {
		if ( remoteNeo4j != null ) {
			remoteNeo4j.close();
			remoteNeo4j = null;
		}
	}

	@Override
	public void start() {
		if ( remoteNeo4j == null ) {
			try {
				remoteNeo4j = new Neo4jClient( getDatabase() );
				sequenceGenerator = new RemoteSequenceGenerator( remoteNeo4j, sequenceCacheMaxSize );
			}
			catch (HibernateException e) {
				// Wrap HibernateException in a ServiceException to make the stack trace more friendly
				// Otherwise a generic unable to request service is thrown
				throw logger.unableToStartDatastoreProvider( e );
			}
		}
	}

	@Override
	public boolean allowsTransactionEmulation() {
		return false;
	}

	public Neo4jClient getDataStore() {
		return remoteNeo4j;
	}

	private DatabaseIdentifier getDatabase() {
		if ( !configuration.getHosts().isSingleHost() ) {
			logger.doesNotSupportMultipleHosts( configuration.getHosts().toString() );
		}
		Hosts.HostAndPort hostAndPort = configuration.getHosts().getFirst();
		try {
			return new DatabaseIdentifier( hostAndPort.getHost(), hostAndPort.getPort(), configuration.getDatabaseName(), configuration.getUsername(),
					configuration.getPassword() );
		}
		catch (Exception e) {
			throw logger.malformedDataBaseUrl( e, hostAndPort.getHost(), hostAndPort.getPort(), configuration.getDatabaseName() );
		}
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return RemoteNeo4jSchemaDefiner.class;
	}

	public RemoteSequenceGenerator getSequenceGenerator() {
		return sequenceGenerator;
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder(TransactionCoordinatorBuilder coordinatorBuilder) {
		return new RemoteTransactionCoordinatorBuilder( coordinatorBuilder, this );
	}
}
