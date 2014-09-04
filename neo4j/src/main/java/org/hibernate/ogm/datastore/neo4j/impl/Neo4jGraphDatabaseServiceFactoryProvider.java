/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.impl;

import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.datastore.neo4j.spi.GraphDatabaseServiceFactory;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Creates an instance of {@link GraphDatabaseServiceFactory} using the implementation selected in the properties.
 * <p>
 * If an implementation is not selected the default one is {@link EmbeddedGraphDatabaseFactory}.
 *
 * @see GraphDatabaseServiceFactory
 * @see InternalProperties#NEO4J_GRAPHDB_FACTORYCLASS
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jGraphDatabaseServiceFactoryProvider {

	public GraphDatabaseServiceFactory load(Map<?, ?> properties, ClassLoaderService classLoaderService) {
		GraphDatabaseServiceFactory factory = new ConfigurationPropertyReader(properties, classLoaderService )
			.property( InternalProperties.NEO4J_GRAPHDB_FACTORYCLASS, GraphDatabaseServiceFactory.class )
			.instantiate()
			.withDefaultImplementation( EmbeddedGraphDatabaseFactory.class )
			.getValue();

		factory.initialize( properties );

		return factory;
	}
}
