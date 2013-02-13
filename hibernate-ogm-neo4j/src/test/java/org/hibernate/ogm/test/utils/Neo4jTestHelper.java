/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.test.utils;

import static org.hibernate.ogm.datastore.neo4j.Environment.NEO4J_DATABASE_PATH;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.TupleSnapshot;
import org.hibernate.ogm.dialect.neo4j.Neo4jDialect;
import org.hibernate.ogm.dialect.neo4j.Neo4jJtaPlatform;
import org.hibernate.ogm.grid.EntityKey;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jTestHelper implements TestableGridDialect {

	@Override
	public boolean assertNumberOfEntities(int numberOfEntities, SessionFactory sessionFactory) {
		return numberOfEntities == countEntities( sessionFactory );
	}

	@Override
	public boolean assertNumberOfAssociations(int numberOfAssociations, SessionFactory sessionFactory) {
		return numberOfAssociations == countAssociations( sessionFactory );
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
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put( Environment.JTA_PLATFORM, Neo4jJtaPlatform.class.getName() );
		properties.put( NEO4J_DATABASE_PATH, dbLocation() );
		return properties;
	}

	/**
	 * Returns a random location where to create a neo4j database
	 */
	public static String dbLocation() {
		try {
			Properties hibProperties = new Properties();
			hibProperties.load( Thread.currentThread().getContextClassLoader()
					.getResourceAsStream( "hibernate.properties" ) );
			String buildDirectory = hibProperties.getProperty( NEO4J_DATABASE_PATH );
			return buildDirectory + File.separator + "neo4j-db-" + System.currentTimeMillis();
		}
		catch ( IOException e ) {
			throw new RuntimeException( "Missing properties file: hibernate.properties" );
		}
	}

	private static Neo4jDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService(
				DatastoreProvider.class );
		if ( !( Neo4jDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Neo4jDB, cannot extract underlying provider" );
		}
		return Neo4jDatastoreProvider.class.cast( provider );
	}

	public int countAssociations(SessionFactory sessionFactory) {
		Iterable<Relationship> relationships = getProvider( sessionFactory ).getAllRelationships();
		Set<String> associations = new HashSet<String>();
		for ( Relationship relationship : relationships ) {
			if ( !associations.contains( relationship.getType().name() ) ) {
				associations.add( relationship.getType().name() );
			}
		}
		return associations.size();
	}

	public int countEntities(SessionFactory sessionFactory) {
		Iterable<Node> nodes = getProvider( sessionFactory ).getAllNodes();
		int count = 0;
		for ( Node node : nodes ) {
			if ( node.hasProperty( Neo4jDialect.TABLE_PROPERTY ) ) {
				count++;
			}
		}
		return count;
	}

}
