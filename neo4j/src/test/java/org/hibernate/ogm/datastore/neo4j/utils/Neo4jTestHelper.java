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
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.fest.util.Files;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.neo4j.Neo4j;
import org.hibernate.ogm.datastore.neo4j.Neo4jDialect;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.utils.TestableGridDialect;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jTestHelper implements TestableGridDialect {

	private static final String ROOT_FOLDER = buildDirectory() + File.separator + "NEO4J";

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		String allEntitiesQuery = Neo4jDialect.TABLE_PROPERTY + ":*";
		ResourceIterator<Node> iterator = getProvider( sessionFactory ).getNodesIndex().query( allEntitiesQuery ).iterator();
		int count = 0;
		while ( iterator.hasNext() ) {
			iterator.next();
			count++;
		}
		iterator.close();
		return count;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		ResourceIterator<Relationship> relationships = getProvider( sessionFactory ).getRelationshipsIndex().query( "*:*" ).iterator();
		Set<String> associations = new HashSet<String>();
		while ( relationships.hasNext() ) {
			Relationship relationship = relationships.next();
			if ( !associations.contains( relationship.getType().name() ) ) {
				associations.add( relationship.getType().name() );
			}
		}
		return associations.size();
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		Map<String, Object> tuple = new HashMap<String, Object>();
		Neo4jDialect dialect = new Neo4jDialect( getProvider( sessionFactory ) );
		TupleSnapshot snapshot = dialect.getTuple( key, null ).getSnapshot();
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
	public long getNumberOEmbeddedCollections(SessionFactory sessionFactory) {
		throw new UnsupportedOperationException( "This datastore does not support storing collections embedded within entities." );
	}

	@Override
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return configuration.configureOptionsFor( Neo4j.class );
	}
}
