/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.utils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

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
import org.hibernate.ogm.datastore.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.TupleContext;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.AnnotationOptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.utils.TestableGridDialect;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.kernel.GraphDatabaseAPI;

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
		GraphDatabaseService neo4jDb = getProvider( sessionFactory ).getDataBase();
		TransactionManager txManager = transactionManager( neo4jDb );
		Transaction suspend = null;
		try {
			suspend = txManager.suspend();
			if ( suspend == null ) {
				txManager.begin();
			}
			else {
				txManager.resume( suspend );
			}
			ExecutionEngine engine = new ExecutionEngine( neo4jDb );
			ExecutionResult result = engine.execute( ENTITY_COUNT_QUERY );
			ResourceIterator<Map<String, Object>> iterator = result.iterator();
			try {
				if (suspend == null) {
					txManager.commit();
				}
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
			if ( iterator.hasNext() ) {
				Map<String, Object> next = iterator.next();
				return ( (Long) next.get( "COUNT(n)" ) ).longValue();
			}
			return 0;
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	@SuppressWarnings("deprecation")
	private TransactionManager transactionManager(GraphDatabaseService neo4jDb) {
		return ( (GraphDatabaseAPI) neo4jDb ).getDependencyResolver().resolveDependency( TransactionManager.class );
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		GraphDatabaseService neo4jDb = getProvider( sessionFactory ).getDataBase();
		TransactionManager txManager = transactionManager( neo4jDb );
		Transaction suspend = null;
		try {
			suspend = txManager.suspend();
			if ( suspend == null ) {
				txManager.begin();
			}
			else {
				txManager.resume( suspend );
			}
			String query = "MATCH (n) - [r] -> () RETURN COUNT(DISTINCT type(r))";
			ExecutionEngine engine = new ExecutionEngine( neo4jDb );
			ExecutionResult result = engine.execute( query.toString() );
			ResourceIterator<Long> columnAs = result.columnAs( "COUNT(DISTINCT type(r))" );
			Long next = columnAs.next();
			columnAs.close();
			try {
				if (suspend == null) {
					txManager.commit();
				}
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
			return next.longValue();
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		Map<String, Object> tuple = new HashMap<String, Object>();
		Neo4jDialect dialect = new Neo4jDialect( getProvider( sessionFactory ) );
		TupleSnapshot snapshot = dialect.getTuple( key, getEmptyTupleContext() ).getSnapshot();
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

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		throw new UnsupportedOperationException( "This datastore does not support different association storage strategies." );
	}

	@Override
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return configuration.configureOptionsFor( Neo4j.class );
	}

	private TupleContext getEmptyTupleContext() {
		return new TupleContext(
				Collections.<String>emptyList(),
				Collections.<String, AssociatedEntityKeyMetadata>emptyMap(),
				Collections.<String, String>emptyMap(),
				OptionsContextImpl.forEntity( Arrays.<OptionValueSource>asList( new AnnotationOptionValueSource() ), Object.class )
		);
	}
}
