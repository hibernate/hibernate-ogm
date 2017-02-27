/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.embedded.impl;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
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
			return builder.newGraphDatabase();
		}
		finally {
			currentThread.setContextClassLoader( contextClassLoader );
		}
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
}
