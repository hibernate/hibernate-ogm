/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.util.Collections;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.TableGenerator;

import org.fest.util.Files;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.ogm.boot.OgmSessionFactoryBuilder;
import org.hibernate.ogm.datastore.neo4j.Neo4jDialect;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.impl.DefaultIdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
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

	@Before
	public void setUp() {
		dbLocation = Neo4jTestHelper.dbLocation();

		StandardServiceRegistry serviceRegistry = TestHelper.getDefaultTestStandardServiceRegistry(
				Collections.<String, Object>singletonMap( Neo4jProperties.DATABASE_PATH, dbLocation )
		);

		new MetadataSources( serviceRegistry )
			.addAnnotatedClass( EntityWithTableGenerator.class )
			.addAnnotatedClass( AnotherEntityWithTableGenerator.class )
			.buildMetadata()
			.getSessionFactoryBuilder()
			.unwrap( OgmSessionFactoryBuilder.class )
			.build();

		dialect = (Neo4jDialect) serviceRegistry.getService( GridDialect.class );
	}

	@After
	public void tearDown() {
		Files.delete( new File( dbLocation ) );
	}

	@Test
	public void testFirstValueIsInitialValue() {
		final IdSourceKey generatorKey = buildIdGeneratorKey( INITIAL_VALUE_SEQUENCE );
		Number sequenceValue = dialect.nextValue( new NextValueRequest( generatorKey, 1, INITIAL_VALUE_FIRST_VALUE_TEST ) );
		assertThat( sequenceValue ).isEqualTo( Long.valueOf( INITIAL_VALUE_FIRST_VALUE_TEST ) );
	}

	@Test
	public void testThreadSafety() throws InterruptedException {
		final IdSourceKey generatorKey = buildIdGeneratorKey( THREAD_SAFETY_SEQUENCE );
		Thread[] threads = new Thread[THREADS];
		for ( int i = 0; i < threads.length; i++ ) {
			threads[i] = new Thread( new Runnable() {
				@Override
				public void run() {
					for ( int i = 0; i < LOOPS; i++ ) {
						dialect.nextValue( new NextValueRequest( generatorKey, 1, INITIAL_VALUE_THREAD_SAFETY_TEST ) );
					}
				}
			} );
			threads[i].start();
		}
		for ( Thread thread : threads ) {
			thread.join();
		}
		Number value = dialect.nextValue( new NextValueRequest( generatorKey, 0, 1 ) );
		assertThat( value ).isEqualTo( Long.valueOf( LOOPS * THREADS ) );
	}

	private IdSourceKey buildIdGeneratorKey(String sequenceName) {
		IdSourceKeyMetadata metadata = DefaultIdSourceKeyMetadata.forTable( HIBERNATE_SEQUENCES, "sequence_name", "next_val" );
		return IdSourceKey.forTable( metadata, sequenceName );
	}

	@Entity
	private static class EntityWithTableGenerator {

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "gen1")
		@TableGenerator(
			name = "gen1",
			initialValue = INITIAL_VALUE_FIRST_VALUE_TEST,
			pkColumnValue = INITIAL_VALUE_SEQUENCE
		)
		Long id;
	}

	@Entity
	private static class AnotherEntityWithTableGenerator {

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "gen2")
		@TableGenerator(
			name = "gen2",
			initialValue = INITIAL_VALUE_THREAD_SAFETY_TEST,
			pkColumnValue = THREAD_SAFETY_SEQUENCE
		)
		Long id;
	}
}
