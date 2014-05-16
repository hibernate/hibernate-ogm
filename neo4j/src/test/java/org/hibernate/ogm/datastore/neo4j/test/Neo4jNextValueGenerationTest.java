/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Properties;

import org.fest.util.Files;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.ogm.datastore.neo4j.Neo4jDialect;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jNextValueGenerationTest {

	private static final int LOOPS = 2;
	private static final int THREADS = 10;

	private Neo4jDialect dialect;

	private String dbLocation;

	private Neo4jDatastoreProvider provider;

	@Before
	public void setUp() {
		dbLocation = Neo4jTestHelper.dbLocation();
		Properties configurationValues = new Properties();
		configurationValues.put( Neo4jProperties.DATABASE_PATH, dbLocation );
		provider = new Neo4jDatastoreProvider();

		ServiceRegistryImplementor serviceRegistry = mock( ServiceRegistryImplementor.class );
		when( serviceRegistry.getService( ClassLoaderService.class ) ).thenReturn( new ClassLoaderServiceImpl() );
		provider.injectServices( serviceRegistry );

		provider.configure( configurationValues );
		provider.start();
		dialect = new Neo4jDialect( provider );
	}

	@After
	public void tearDown() {
		provider.stop();
		Files.delete( new File( dbLocation ) );
	}

	@Test
	public void testFirstValueIsInitialValue() {
		final int initialValue = 5;
		final RowKey sequenceNode = new RowKey( "initialSequence", new String[0], new Object[0] );
		final IdentifierGeneratorHelper.BigIntegerHolder sequenceValue = new IdentifierGeneratorHelper.BigIntegerHolder();
		dialect.nextValue( sequenceNode, sequenceValue, 1, initialValue );
		assertThat( sequenceValue.makeValue().intValue(), equalTo( initialValue ) );
	}

	@Test
	public void testThreadSafty() throws InterruptedException {
		final RowKey test = new RowKey( "test", new String[0], new Object[0] );
		Thread[] threads = new Thread[THREADS];
		for ( int i = 0; i < threads.length; i++ ) {
			threads[i] = new Thread( new Runnable() {
				@Override
				public void run() {
					final IdentifierGeneratorHelper.BigIntegerHolder value = new IdentifierGeneratorHelper.BigIntegerHolder();
					for ( int i = 0; i < LOOPS; i++ ) {
						dialect.nextValue( test, value, 1, 0 );
					}
				}
			} );
			threads[i].start();
		}
		for ( Thread thread : threads ) {
			thread.join();
		}
		final IdentifierGeneratorHelper.BigIntegerHolder value = new IdentifierGeneratorHelper.BigIntegerHolder();
		dialect.nextValue( test, value, 0, 1 );
		assertThat( value.makeValue().intValue(), equalTo( LOOPS * THREADS ) );
	}
}
