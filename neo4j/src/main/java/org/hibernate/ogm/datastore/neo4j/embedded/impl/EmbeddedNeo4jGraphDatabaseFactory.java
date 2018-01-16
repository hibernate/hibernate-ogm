/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.impl;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.neo4j.spi.GraphDatabaseServiceFactory;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Contains methods to create a {@link GraphDatabaseService} for the embedded Neo4j.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class EmbeddedNeo4jGraphDatabaseFactory implements GraphDatabaseServiceFactory {
	private static final Map<String, GraphDatabaseService> GRAPH_DATABASE_SERVICE_MAP = new ConcurrentHashMap<>();
	private static Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private File dbLocation;

	private URL configurationLocation;

	private Map<?, ?> configuration;

	@Override
	public void initialize(Map<?, ?> properties) {
		ConfigurationPropertyReader configurationPropertyReader = new ConfigurationPropertyReader( properties );

		String path = configurationPropertyReader.property( Neo4jProperties.DATABASE_PATH, String.class )
				.required()
				.getValue();

		this.dbLocation = new File( path );

		this.configurationLocation = configurationPropertyReader
				.property( Neo4jProperties.CONFIGURATION_RESOURCE_NAME, URL.class )
				.getValue();

		configuration = properties;
	}

	@Override
	public GraphDatabaseService create() {
		final String dbLocationAbsolutePath = dbLocation.getAbsolutePath();
//		if ( !GRAPH_DATABASE_SERVICE_MAP.containsKey( dbLocationAbsolutePath ) )  {
//			synchronized (GRAPH_DATABASE_SERVICE_MAP) {
//				if ( !GRAPH_DATABASE_SERVICE_MAP.containsKey( dbLocationAbsolutePath ) )  {
//					LOG.infof( " Create new service instance for dbPath  %s", dbLocationAbsolutePath );
//					GraphDatabaseService service = buildGraphDatabaseService();
//					GRAPH_DATABASE_SERVICE_MAP.put( dbLocationAbsolutePath, service );
//				}
//			}
//		}
		//return  GRAPH_DATABASE_SERVICE_MAP.get( dbLocationAbsolutePath );
		return GRAPH_DATABASE_SERVICE_MAP.computeIfAbsent( dbLocationAbsolutePath, ( dbPath ) -> {
			LOG.infof( " Create new service instance for dbPath  %s", dbLocationAbsolutePath );
			return buildGraphDatabaseService();
		} );
	}

	private GraphDatabaseService buildGraphDatabaseService() {
		GraphDatabaseService service = null;
		GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder( dbLocation );
		setConfigurationFromLocation( builder, configurationLocation );
		setConfigurationFromProperties( builder, configuration );
		final ClassLoader neo4JClassLoader = builder.getClass().getClassLoader();
		final Thread currentThread = Thread.currentThread();
		final ClassLoader contextClassLoader = currentThread.getContextClassLoader();
		try {
			//Neo4J relies on the context classloader to load its own extensions:
			//Allow it to load even the ones we're not exposing directly to end users.
			currentThread.setContextClassLoader( neo4JClassLoader );
			service =  builder.newGraphDatabase();
		}
		catch (Exception e) {
			throw LOG.cannotCreateNewGraphDatabaseServiceException( e );
		}
		finally {
			currentThread.setContextClassLoader( contextClassLoader );
		}
		return service;
	}

	private void setConfigurationFromProperties(GraphDatabaseBuilder builder, Map<?, ?> properties) {
		if ( properties != null ) {
			builder.setConfig( convert( properties ) );
		}
	}

	private Map<String, String> convert(Map<?, ?> properties) {
		Map<String, String> neo4jConfiguration = new HashMap<String, String>();
		for ( Map.Entry<?, ?> entry : properties.entrySet() ) {
			neo4jConfiguration.put( String.valueOf( entry.getKey() ), String.valueOf( entry.getValue() ) );
		}
		return neo4jConfiguration;
	}

	private void setConfigurationFromLocation(GraphDatabaseBuilder builder, URL cfgLocation) {
		if ( cfgLocation != null ) {
			builder.loadPropertiesFromURL( cfgLocation );
		}
	}

	static void shutdownGraphDatabaseService(GraphDatabaseService neo4jDb) {
		LOG.info( " Shutdown service instance" );
		String key = null;
		for ( String currentKey : GRAPH_DATABASE_SERVICE_MAP.keySet() ) {
			if ( GRAPH_DATABASE_SERVICE_MAP.get( currentKey ) == neo4jDb ) {
				key = currentKey;
				break;
			}
		}
		GRAPH_DATABASE_SERVICE_MAP.remove( key );
		neo4jDb.shutdown();
	}
}
