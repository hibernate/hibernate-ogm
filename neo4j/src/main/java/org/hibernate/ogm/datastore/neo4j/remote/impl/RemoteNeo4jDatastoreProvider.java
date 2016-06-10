/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.spi.Hosts;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.RemoteNeo4jDialect;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jBasedQueryParserService;
import org.hibernate.ogm.datastore.neo4j.remote.dialect.impl.RemoteNeo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.remote.transaction.impl.RemoteNeo4jTransactionCoordinatorBuilder;
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

	private static final int OK = 200;

	private static final int DEFAULT_SEQUENCE_QUERY_CACHE_MAX_SIZE = 128;

	private static final Log logger = LoggerFactory.getLogger();

	private Integer sequenceCacheMaxSize;

	private RemoteNeo4jConfiguration configuration;

	private RemoteNeo4jClient remoteNeo4j;

	private RemoteNeo4jSequenceGenerator sequenceGenerator;

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
		configuration = new RemoteNeo4jConfiguration( new ConfigurationPropertyReader( configurationValues ) );
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
				remoteNeo4j = createNeo4jClient( getDatabaseIdentifier(), configuration );
				validateCredentials( remoteNeo4j, configuration );
				sequenceGenerator = new RemoteNeo4jSequenceGenerator( remoteNeo4j, sequenceCacheMaxSize );
			}
			catch (HibernateException e) {
				// Wrap HibernateException in a ServiceException to make the stack trace more friendly
				// Otherwise a generic unable to request service is thrown
				throw logger.unableToStartDatastoreProvider( e );
			}
		}
	}

	private void validateCredentials(RemoteNeo4jClient client, RemoteNeo4jConfiguration configuration) {
		Response response = client.authenticate( configuration.getUsername() );
		if ( response.getStatus() != OK ) {
			throw logger.authenticationFailed( String.valueOf( configuration.getHosts() ), response.getStatus(), response.getStatusInfo().getReasonPhrase() );
		}
	}

	/**
	 * Creates the {@link RemoteNeo4jClient} that it is going to be used to connect to a remote Neo4j server.
	 *
	 * @param database the connection properties to identify a database
	 * @param configuration all the configuration properties
	 * @return a client that can access a Neo4j server
	 */
	public RemoteNeo4jClient createNeo4jClient(RemoteNeo4jDatabaseIdentifier database, RemoteNeo4jConfiguration configuration) {
		return new RemoteNeo4jClient( getDatabaseIdentifier(), configuration );
	}

	@Override
	public boolean allowsTransactionEmulation() {
		// This value does not really matter since we are using a custom TransactionCoordinatorBuilder
		return true;
	}

	// Note that it's called getDatabase() for consistency with the other Neo4j provider
	public RemoteNeo4jClient getDatabase() {
		return remoteNeo4j;
	}

	private RemoteNeo4jDatabaseIdentifier getDatabaseIdentifier() {
		if ( !configuration.getHosts().isSingleHost() ) {
			logger.doesNotSupportMultipleHosts( configuration.getHosts().toString() );
		}
		Hosts.HostAndPort hostAndPort = configuration.getHosts().getFirst();
		try {
			return new RemoteNeo4jDatabaseIdentifier( hostAndPort.getHost(), hostAndPort.getPort(), configuration.getDatabaseName(), configuration.getUsername(),
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

	public RemoteNeo4jSequenceGenerator getSequenceGenerator() {
		return sequenceGenerator;
	}

	@Override
	public TransactionCoordinatorBuilder getTransactionCoordinatorBuilder(TransactionCoordinatorBuilder coordinatorBuilder) {
		return new RemoteNeo4jTransactionCoordinatorBuilder( coordinatorBuilder, this );
	}
}
