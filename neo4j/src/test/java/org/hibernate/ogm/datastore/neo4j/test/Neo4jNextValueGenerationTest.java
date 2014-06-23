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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.fest.util.Files;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.DefaultNamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.ogm.datastore.neo4j.Neo4jDialect;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.impl.Neo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper;
import org.hibernate.ogm.dialect.NoopDialect;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.IdGeneratorKeyMetadata;
import org.hibernate.ogm.id.impl.OgmTableGenerator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.LongType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class Neo4jNextValueGenerationTest {

	private static final String HIBERNATE_SEQUENCES = "hibernate_sequences";
	private static final String THREAD_SAFETY_SEQUENCE = "ThreadSafetySequence";
	private static final String INITIAL_VALUE_SEQUENCE = "InitialValueSequence";

	private static final int INITIAL_VALUE_FIRST_VALUE_TEST = 5;
	private static final int INITIAL_VALUE_THREAD_SAFETY_TEST = 0;

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

		Set<IdentifierGenerator> sequences = new HashSet<IdentifierGenerator>();
		sequences.add( identifierGenerator( INITIAL_VALUE_SEQUENCE, INITIAL_VALUE_FIRST_VALUE_TEST ) );
		sequences.add( identifierGenerator( THREAD_SAFETY_SEQUENCE, INITIAL_VALUE_THREAD_SAFETY_TEST ) );

		provider.configure( configurationValues );
		provider.start();
		provider.getSequenceGenerator().createSequences( sequences );
		dialect = new Neo4jDialect( provider );
	}

	private IdentifierGenerator identifierGenerator(String sequenceName, int initialValue) {
		Properties newParams = new Properties();
		newParams.setProperty( OgmTableGenerator.SEGMENT_VALUE_PARAM, sequenceName );
		newParams.setProperty( OgmTableGenerator.TABLE_PARAM, HIBERNATE_SEQUENCES );
		newParams.setProperty( OgmTableGenerator.INITIAL_PARAM, String.valueOf( initialValue ) );
		newParams.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, new DefaultObjectNameNormalizer() );
		OgmTableGenerator tableGenerator = new OgmTableGenerator();
		tableGenerator.configure( LongType.INSTANCE, newParams, new NoopDialect() );
		return tableGenerator;
	}

	private static class DefaultObjectNameNormalizer extends ObjectNameNormalizer {

		@Override
		protected boolean isUseQuotedIdentifiersGlobally() {
			return false;
		}

		@Override
		protected NamingStrategy getNamingStrategy() {
			return new DefaultNamingStrategy();
		}
	}

	@After
	public void tearDown() {
		provider.stop();
		Files.delete( new File( dbLocation ) );
	}

	@Test
	public void testFirstValueIsInitialValue() {
		final IdGeneratorKey generatorKey = buildIdGeneratorKey( INITIAL_VALUE_SEQUENCE );
		final IdentifierGeneratorHelper.BigIntegerHolder sequenceValue = new IdentifierGeneratorHelper.BigIntegerHolder();
		dialect.nextValue( generatorKey, sequenceValue, 1, INITIAL_VALUE_FIRST_VALUE_TEST );
		assertThat( sequenceValue.makeValue().intValue(), equalTo( INITIAL_VALUE_FIRST_VALUE_TEST ) );
	}

	@Test
	public void testThreadSafety() throws InterruptedException {
		final IdGeneratorKey generatorKey = buildIdGeneratorKey( THREAD_SAFETY_SEQUENCE );
		Thread[] threads = new Thread[THREADS];
		for ( int i = 0; i < threads.length; i++ ) {
			threads[i] = new Thread( new Runnable() {
				@Override
				public void run() {
					final IdentifierGeneratorHelper.BigIntegerHolder value = new IdentifierGeneratorHelper.BigIntegerHolder();
					for ( int i = 0; i < LOOPS; i++ ) {
						dialect.nextValue( generatorKey, value, 1, INITIAL_VALUE_THREAD_SAFETY_TEST );
					}
				}
			} );
			threads[i].start();
		}
		for ( Thread thread : threads ) {
			thread.join();
		}
		final IdentifierGeneratorHelper.BigIntegerHolder value = new IdentifierGeneratorHelper.BigIntegerHolder();
		dialect.nextValue( generatorKey, value, 0, 1 );
		assertThat( value.makeValue().intValue(), equalTo( LOOPS * THREADS ) );
	}


	private IdGeneratorKey buildIdGeneratorKey(String sequenceName) {
		IdGeneratorKeyMetadata metadata = IdGeneratorKeyMetadata.forTable( HIBERNATE_SEQUENCES, "sequence_name", "next_val" );
		return IdGeneratorKey.forTable( metadata, sequenceName );
	}
}
