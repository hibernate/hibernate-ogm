/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fest.util.Files;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.neo4j.Neo4j;
import org.hibernate.ogm.datastore.neo4j.Neo4jDialect;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.utils.GridDialectOperationContexts;
import org.hibernate.ogm.utils.TestableGridDialect;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.ResourceIterator;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class Neo4jTestHelper implements TestableGridDialect {

	/**
	 * Query for counting all entities. This takes embedded entities and temporary nodes (which never should show up
	 * actually) into account.
	 */
	private static final String ENTITY_COUNT_QUERY = "MATCH (n) WHERE n:" + NodeLabel.ENTITY.name() + " OR n:" + NodeLabel.EMBEDDED.name() + " RETURN COUNT(n)";

	private static final String ROOT_FOLDER = buildDirectory() + File.separator + "NEO4J";

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		ExecutionEngine engine = new ExecutionEngine( getProvider( sessionFactory ).getDataBase() );
		ExecutionResult result = engine.execute( ENTITY_COUNT_QUERY );
		ResourceIterator<Map<String, Object>> iterator = result.iterator();
		if ( iterator.hasNext() ) {
			Map<String, Object> next = iterator.next();
			return ( (Long) next.get( "COUNT(n)" ) ).longValue();
		}
		return 0;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		String query = "MATCH (n) - [r] -> () RETURN COUNT(DISTINCT type(r))";
		ExecutionEngine engine = new ExecutionEngine( getProvider( sessionFactory ).getDataBase() );
		ExecutionResult result = engine.execute( query.toString() );
		ResourceIterator<Long> columnAs = result.columnAs( "COUNT(DISTINCT type(r))" );
		Long next = columnAs.next();
		columnAs.close();
		return next.longValue();
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		Map<String, Object> tuple = new HashMap<String, Object>();
		GridDialect dialect = getDialect( sessionFactory );
		TupleSnapshot snapshot = dialect.getTuple( key, GridDialectOperationContexts.emptyTupleContext() ).getSnapshot();
		for ( String column : snapshot.getColumnNames() ) {
			tuple.put( column, snapshot.get( column ) );
		}
		return tuple;
	}

	@Override
	public boolean backendSupportsTransactions() {
		return true;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		getProvider( sessionFactory ).stop();
		Files.delete( new File( ROOT_FOLDER ) );
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put( Neo4jProperties.DATABASE_PATH, dbLocation() );
		return properties;
	}

	/**
	 * Returns a random location where to create a neo4j database
	 */
	public static String dbLocation() {
		return ROOT_FOLDER + File.separator + "neo4j-db-" + System.currentTimeMillis();
	}

	private static String buildDirectory() {
		try {
			Properties hibProperties = new Properties();
			hibProperties.load( Thread.currentThread().getContextClassLoader().getResourceAsStream( "hibernate.properties" ) );
			String buildDirectory = hibProperties.getProperty( Neo4jProperties.DATABASE_PATH );
			return buildDirectory;
		}
		catch (IOException e) {
			throw new RuntimeException( "Missing properties file: hibernate.properties" );
		}
	}

	private static Neo4jDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( !( Neo4jDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Neo4jDB, cannot extract underlying provider" );
		}
		return Neo4jDatastoreProvider.class.cast( provider );
	}

	private static GridDialect getDialect(SessionFactory sessionFactory) {
		GridDialect dialect = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( GridDialect.class );
		return dialect;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		throw new UnsupportedOperationException( "This datastore does not support different association storage strategies." );
	}

	@Override
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return configuration.configureOptionsFor( Neo4j.class );
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new Neo4jDialect( (Neo4jDatastoreProvider) datastoreProvider );
	}
}
