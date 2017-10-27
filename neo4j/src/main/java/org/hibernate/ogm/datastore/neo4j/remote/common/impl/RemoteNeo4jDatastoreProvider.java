/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.common.impl;

import java.util.Map;

import org.hibernate.ogm.cfg.spi.Hosts;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.ogm.datastore.neo4j.query.parsing.impl.Neo4jBasedQueryParserService;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.query.spi.QueryParserService;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * @author Davide D'Alto
 */
public abstract class RemoteNeo4jDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	private static final Log logger = LoggerFactory.make( MethodHandles.lookup() );

	private static final int DEFAULT_SEQUENCE_QUERY_CACHE_MAX_SIZE = 128;

	protected RemoteNeo4jConfiguration configuration;

	private final int defaultPort;

	private final String protocol;

	private Integer sequenceCacheMaxSize;

	protected RemoteNeo4jDatastoreProvider( String protocol, int defaultPort ) {
		this.protocol = protocol;
		this.defaultPort = defaultPort;
	}

	public abstract Object getSequenceGenerator();

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return Neo4jBasedQueryParserService.class;
	}

	@Override
	public void configure(Map configurationValues) {
		configuration = new RemoteNeo4jConfiguration( new ConfigurationPropertyReader( configurationValues ), defaultPort );
		sequenceCacheMaxSize = new ConfigurationPropertyReader( configurationValues )
				.property( Neo4jProperties.SEQUENCE_QUERY_CACHE_MAX_SIZE, int.class )
				.withDefault( DEFAULT_SEQUENCE_QUERY_CACHE_MAX_SIZE )
				.getValue();
	}

	@Override
	public boolean allowsTransactionEmulation() {
		// Remote Neo4j supports transaction so we don't need to emulate them
		return false;
	}

	protected RemoteNeo4jDatabaseIdentifier getDatabaseIdentifier() {
		if ( !configuration.getHosts().isSingleHost() ) {
			logger.doesNotSupportMultipleHosts( configuration.getHosts().toString() );
		}
		Hosts.HostAndPort hostAndPort = configuration.getHosts().getFirst();
		try {
			return new RemoteNeo4jDatabaseIdentifier( protocol, configuration );
		}
		catch (Exception e) {
			throw logger.malformedDataBaseUrl( e, hostAndPort.getHost(), hostAndPort.getPort(), configuration.getDatabaseName() );
		}
	}

	public Integer getSequenceCacheMaxSize() {
		return sequenceCacheMaxSize;
	}
}
