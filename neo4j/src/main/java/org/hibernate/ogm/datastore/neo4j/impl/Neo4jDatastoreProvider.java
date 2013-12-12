/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.impl;

import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.ogm.datastore.neo4j.Neo4jDialect;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.dialect.Neo4jSequenceGenerator;
import org.hibernate.ogm.datastore.neo4j.spi.GraphDatabaseServiceFactory;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.service.impl.LuceneBasedQueryParserService;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;

/**
 * Provides access to the Neo4j system.
 *
 * @author Davide D'Alto
 */
public class Neo4jDatastoreProvider implements DatastoreProvider, Startable, Stoppable, Configurable, ServiceRegistryAwareService {

	/**
	 * Default name of the index that stores entities.
	 */
	private static final String DEFAULT_NEO4J_ENTITY_INDEX_NAME = "_nodes_ogm_index";

	/**
	 * Default name of the index that stores associations.
	 */
	private static final String DEFAULT_NEO4J_ASSOCIATION_INDEX_NAME = "_relationships_ogm_index";

	/**
	 * Default Name of the index that stores the next available value for sequences.
	 */
	private static final String DEFAULT_NEO4J_SEQUENCE_INDEX_NAME = "_sequences_ogm_index";

	private String sequenceIndexName = DEFAULT_NEO4J_SEQUENCE_INDEX_NAME;

	private String nodeIndexName = DEFAULT_NEO4J_ENTITY_INDEX_NAME;

	private String relationshipIndexName = DEFAULT_NEO4J_ASSOCIATION_INDEX_NAME;

	private GraphDatabaseService neo4jDb;

	private Neo4jSequenceGenerator neo4jSequenceGenerator;

	private GraphDatabaseServiceFactory graphDbFactory;

	private ServiceRegistryImplementor registry;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.registry = serviceRegistry;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return LuceneBasedQueryParserService.class;
	}

	@Override
	public void configure(Map cfg) {
		graphDbFactory = new Neo4jGraphDatabaseServiceFactoryProvider().load( cfg, registry.getService( ClassLoaderService.class ) );
		sequenceIndexName = defaultIfNull( cfg, Neo4jProperties.SEQUENCE_INDEX_NAME, DEFAULT_NEO4J_SEQUENCE_INDEX_NAME );
		nodeIndexName = defaultIfNull( cfg, Neo4jProperties.ENTITY_INDEX_NAME, DEFAULT_NEO4J_ENTITY_INDEX_NAME );
		relationshipIndexName = defaultIfNull( cfg, Neo4jProperties.ASSOCIATION_INDEX_NAME, DEFAULT_NEO4J_ASSOCIATION_INDEX_NAME );
	}

	private String defaultIfNull(Map<?, ?> cfg, String key, String defaultValue) {
		String indexName = (String) cfg.get( key );
		return indexName == null ? defaultValue : indexName;
	}

	@Override
	public void stop() {
		neo4jDb.shutdown();
	}

	@Override
	public void start() {
		this.neo4jDb = graphDbFactory.create();
		this.neo4jSequenceGenerator = new Neo4jSequenceGenerator( neo4jDb, sequenceIndexName );
		this.graphDbFactory = null;
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return Neo4jDialect.class;
	}

	public Node createNode() {
		return neo4jDb.createNode();
	}

	public GraphDatabaseService getDataBase() {
		return neo4jDb;
	}

	public int nextValue(RowKey key, int increment, int initialValue) {
		return neo4jSequenceGenerator.nextValue( key, increment, initialValue );
	}

	public Index<Node> getNodesIndex() {
		return neo4jDb.index().forNodes( nodeIndexName );
	}

	public Index<Relationship> getRelationshipsIndex() {
		return neo4jDb.index().forRelationships( relationshipIndexName );
	}
}
