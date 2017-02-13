/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.junit.Test;

/**
 * Test that the generation of sequences using the table strategy is thread safe.
 *
 * @author Davide D'Alto
 */
public class SequenceNextValueGenerationTest extends TestNextValueGeneration {

	private static final String INITIAL_VALUE_SEQUENCE = "InitialValueSequence";
	private static final String THREAD_SAFETY_SEQUENCE = "ThreadSafetySequence";

	private static final int INITIAL_VALUE_TEST_FIRST_VALUE = 5;
	private static final int INITIAL_VALUE_INCREMENT = 1;

	private static final int THREAD_SAFETY_FIRST_VALUE = 12;
	private static final int THREAD_SAFETY_INCREMENT = 3;

	@Override
	protected IdSourceKey buildIdGeneratorKey(Class<?> entityClass, String sequenceName) {
		IdentifierGenerator metadata = generateKeyMetadata( entityClass );
		IdSourceKeyMetadata sequenceMetadata = ( (OgmSequenceGenerator) metadata ).getGeneratorKeyMetadata();
		if ( dialect.supportsSequences() ) {
			return IdSourceKey.forSequence( sequenceMetadata );
		}
		else {
			// Fallback to table generators
			return IdSourceKey.forTable( sequenceMetadata, sequenceName );
		}
	}

	@Test
	public void testFirstValueIsInitialValue() {
		final IdSourceKey generatorKey = buildIdGeneratorKey( InitialValueEntity.class, INITIAL_VALUE_SEQUENCE );
		Number sequenceValue = dialect.nextValue( new NextValueRequest( generatorKey, 1, INITIAL_VALUE_TEST_FIRST_VALUE ) );
		assertThat( sequenceValue.longValue() ).isEqualTo( Long.valueOf( INITIAL_VALUE_TEST_FIRST_VALUE ) );
	}

	@Test
	@SkipByGridDialect(value = GridDialectType.INFINISPAN)
	public void testIncrements() throws InterruptedException {
		final IdSourceKey generatorKey = buildIdGeneratorKey( ThreadSafetyEntity.class, THREAD_SAFETY_SEQUENCE );
		final NextValueRequest nextValueRequest = new NextValueRequest( generatorKey, THREAD_SAFETY_INCREMENT, THREAD_SAFETY_FIRST_VALUE );

		IncrementJob[] runJobs = runJobs( nextValueRequest );

		// Verify we got unique results:
		int[] allGeneratedValues = new int[INCREASES_PER_TASK * NUMBER_OF_TASKS];
		int i = 0;
		for ( IncrementJob job : runJobs ) {
			int[] generatedValuesPerJob = job.retrieveAllGeneratedValues();
			for ( int generatedValue : generatedValuesPerJob ) {
				allGeneratedValues[i++] = generatedValue;
			}
		}
		Arrays.sort( allGeneratedValues );

		int expectedValue = THREAD_SAFETY_FIRST_VALUE;
		for ( int k = 0; k < allGeneratedValues.length; k++ ) {
			assertThat( allGeneratedValues[k] ).as( "Unexpected value generated, index: " + k ).isEqualTo( expectedValue );
			expectedValue += THREAD_SAFETY_INCREMENT;
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ InitialValueEntity.class, ThreadSafetyEntity.class };
	}

	@Entity
	@Table(name = "INITIAL_VALUE_GENERATOR_SEQUENCE")
	private static class InitialValueEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen1")
		@SequenceGenerator( name = "gen1", sequenceName = INITIAL_VALUE_SEQUENCE, initialValue = INITIAL_VALUE_TEST_FIRST_VALUE, allocationSize = INITIAL_VALUE_INCREMENT )
		Long id;
	}

	@Entity
	@Table(name = "THREAD_SAFETY_GENERATOR_SEQUENCE")
	private static class ThreadSafetyEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen2")
		@SequenceGenerator( name = "gen2", sequenceName = THREAD_SAFETY_SEQUENCE, initialValue = THREAD_SAFETY_FIRST_VALUE, allocationSize = THREAD_SAFETY_INCREMENT )
		Long id;
	}
}
