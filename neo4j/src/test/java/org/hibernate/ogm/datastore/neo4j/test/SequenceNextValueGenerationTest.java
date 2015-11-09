/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.impl.DefaultIdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that the generation of sequences using the sequence strategy is thread safe.
 *
 * @author Davide D'Alto
 */
public class SequenceNextValueGenerationTest extends JpaTestCase {

	private static final String THREAD_SAFETY_SEQUENCE = "ThreadSafetySequence";
	private static final String THREAD_SAFETY_SEQUENCE_GEN = THREAD_SAFETY_SEQUENCE + "Gen";

	private static final String INITIAL_VALUE_SEQUENCE = "InitialValueSequence";
	private static final String INITIAL_VALUE_SEQUENCE_GEN = INITIAL_VALUE_SEQUENCE + "Gen";

	private static final int INITIAL_VALUE_FIRST_VALUE_TEST = 5;
	private static final int THREAD_SAFETY_FIRST_VALUE = 12;

	private static final int LOOPS = 2;
	private static final int THREADS = 10;

	private GridDialect dialect;

	@Before
	public void setUp() {
		ServiceRegistryImplementor serviceRegistry = getServiceRegistry();
		dialect = serviceRegistry.getService( GridDialect.class );
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
						dialect.nextValue( new NextValueRequest( generatorKey, 1, THREAD_SAFETY_FIRST_VALUE ) );
					}
				}
			} );
			threads[i].start();
		}
		for ( Thread thread : threads ) {
			thread.join();
		}

		// Here we make another request with different initial and increment values
		Number value = dialect.nextValue( new NextValueRequest( generatorKey, 0, 1 ) );
		assertThat( value ).isEqualTo( Long.valueOf( THREAD_SAFETY_FIRST_VALUE + (LOOPS * THREADS) ) );
	}

	private IdSourceKey buildIdGeneratorKey(String sequenceName) {
		IdSourceKeyMetadata metadata = DefaultIdSourceKeyMetadata.forSequence( sequenceName );
		return IdSourceKey.forSequence( metadata );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { EntityWithSequence.class, AnotherEntityWithSequence.class };
	}

	@Entity
	private static class EntityWithSequence {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = INITIAL_VALUE_SEQUENCE_GEN)
		@SequenceGenerator(
				name = INITIAL_VALUE_SEQUENCE_GEN,
				sequenceName = INITIAL_VALUE_SEQUENCE,
				initialValue = INITIAL_VALUE_FIRST_VALUE_TEST,
				allocationSize = 10
		)
		Long id;
	}

	@Entity
	private static class AnotherEntityWithSequence {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = THREAD_SAFETY_SEQUENCE_GEN)
		@SequenceGenerator(
				name = THREAD_SAFETY_SEQUENCE_GEN,
				sequenceName = THREAD_SAFETY_SEQUENCE,
				initialValue = THREAD_SAFETY_FIRST_VALUE,
				allocationSize = 10
		)
		Long id;
	}
}
