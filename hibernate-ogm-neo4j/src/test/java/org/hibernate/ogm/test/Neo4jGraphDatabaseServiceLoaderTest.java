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
package org.hibernate.ogm.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.hibernate.ogm.datastore.neo4j.Environment;
import org.hibernate.ogm.datastore.neo4j.api.GraphDatabaseServiceFactory;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jGraphDatabaseServiceFactoryProvider;
import org.hibernate.ogm.test.utils.Neo4jTestHelper;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.EmbeddedGraphDatabase;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jGraphDatabaseServiceLoaderTest {

	@Test
	public void testEmbeddedIsTheDefaultGraphDatabaseService() throws Exception {
		Properties properties = new Properties();
		properties.put( Environment.NEO4J_DATABASE_PATH, Neo4jTestHelper.dbLocation() );
		Neo4jGraphDatabaseServiceFactoryProvider graphService = new Neo4jGraphDatabaseServiceFactoryProvider();
		GraphDatabaseService db = graphService.load( properties ).create();
		db.shutdown();
		assertThat( db, is( EmbeddedGraphDatabase.class ) );
	}

	@Test
	public void testSelectedGraphDatabaseServiceIsLoaded() throws Exception {
		Properties properties = new Properties();
		properties.put( Environment.NEO4J_DATABASE_PATH, Neo4jTestHelper.dbLocation() );
		properties.put( Environment.NEO4J_GRAPHDB_FACTORYCLASS, MockGraphServiceFactory.class.getName() );
		Neo4jGraphDatabaseServiceFactoryProvider graphService = new Neo4jGraphDatabaseServiceFactoryProvider();
		GraphDatabaseService db = graphService.load( properties ).create();
		db.shutdown();
		assertThat( db, is( MockGraphDatabaseService.class ) );
	}

	@Test
	public void testPropertiesArePassed() throws Exception {
		Properties properties = new Properties();
		properties.put( Environment.NEO4J_DATABASE_PATH, Neo4jTestHelper.dbLocation() );
		properties.put( Environment.NEO4J_GRAPHDB_FACTORYCLASS, MockGraphServiceFactory.class.getName() );
		Neo4jGraphDatabaseServiceFactoryProvider graphService = new Neo4jGraphDatabaseServiceFactoryProvider();
		MockGraphDatabaseService db = (MockGraphDatabaseService) graphService.load( properties ).create();
		db.shutdown();
		assertTrue( "GraphDatabaseService factory cannot read the configuration properties", db.isConfigurationReadable() );
	}

	public static class MockGraphServiceFactory implements GraphDatabaseServiceFactory {

		boolean configurationReadable = false;

		@Override
		public void initialize(Properties properties) {
			configurationReadable = MockGraphServiceFactory.class.getName().equals(
					properties.get( Environment.NEO4J_GRAPHDB_FACTORYCLASS ) );
		}

		@Override
		public GraphDatabaseService create() {
			return new MockGraphDatabaseService( configurationReadable );
		}

	}

	public static class MockGraphDatabaseService implements GraphDatabaseService {

		private final boolean configurationReadable;

		public MockGraphDatabaseService(boolean readableConfiguration) {
			this.configurationReadable = readableConfiguration;
		}

		public boolean isConfigurationReadable() {
			return configurationReadable;
		}

		@Override
		public Node createNode() {
			return null;
		}

		@Override
		public Node getNodeById(long id) {
			return null;
		}

		@Override
		public Relationship getRelationshipById(long id) {
			return null;
		}

		@Override
		public Node getReferenceNode() {
			return null;
		}

		@Override
		public Iterable<Node> getAllNodes() {
			return null;
		}

		@Override
		public Iterable<RelationshipType> getRelationshipTypes() {
			return null;
		}

		@Override
		public void shutdown() {
		}

		@Override
		public Transaction beginTx() {
			return null;
		}

		@Override
		public <T> TransactionEventHandler<T> registerTransactionEventHandler(TransactionEventHandler<T> handler) {
			return null;
		}

		@Override
		public <T> TransactionEventHandler<T> unregisterTransactionEventHandler(TransactionEventHandler<T> handler) {
			return null;
		}

		@Override
		public KernelEventHandler registerKernelEventHandler(KernelEventHandler handler) {
			return null;
		}

		@Override
		public KernelEventHandler unregisterKernelEventHandler(KernelEventHandler handler) {
			return null;
		}

		@Override
		public IndexManager index() {
			return null;
		}

	}
}
