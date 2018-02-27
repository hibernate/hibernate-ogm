/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.Before;

/**
 * @author Davide D'Alto
 */
public abstract class TestNextValueGeneration extends OgmJpaTestCase {

	protected static boolean RUN_FULL_TESTS = Boolean.getBoolean( "ogm.runFullStressTests" );
	protected static int PARALLEL_THREADS = Runtime.getRuntime().availableProcessors();
	protected static int NUMBER_OF_TASKS = PARALLEL_THREADS * 3;
	protected static int INCREASES_PER_TASK = RUN_FULL_TESTS ? 100_000 : 10;

	protected GridDialect dialect;

	@Before
	public void setUp() {
		ServiceRegistryImplementor serviceRegistry = getServiceRegistry();
		dialect = serviceRegistry.getService( GridDialect.class );
	}

	protected IncrementJob[] runJobs(final NextValueRequest nextValueRequest) throws InterruptedException {
		ExecutorService executorService = Executors.newWorkStealingPool( PARALLEL_THREADS );
		IncrementJob[] runJobs = new IncrementJob[NUMBER_OF_TASKS];

		System.out.println( "Starting stress tests on " + PARALLEL_THREADS + " Threads running " + NUMBER_OF_TASKS + " tasks" );
		// Prepare all jobs (quite a lot of array allocations):
		for ( int i = 0; i < NUMBER_OF_TASKS; i ++ ) {
			runJobs[i] = new IncrementJob( dialect, nextValueRequest );
		}
		// Start them, pretty much in parallel (not really, but we have a lot so they will eventually run in parallel):
		for ( int i = 0; i < NUMBER_OF_TASKS; i ++ ) {
			executorService.execute( runJobs[i] );
		}
		executorService.shutdown();
		executorService.awaitTermination( 10, TimeUnit.MINUTES );
		return runJobs;
	}

	protected abstract IdSourceKey buildIdGeneratorKey(Class<?> entityClass, String sequenceName);

	protected IdentifierGenerator generateKeyMetadata(Class<?> entityClass) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) getFactory();
		IdentifierGenerator generator = sessionFactory.getIdentifierGenerator( entityClass.getName() );
		return generator;
	}

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		TestHelper.enableCountersForInfinispan( info.getProperties() );
	}

	protected static class IncrementJob implements Runnable {

		private final GridDialect dialect;
		private final NextValueRequest nextValueRequest;
		//GuardedBy synchronization on IncrementJob.this :
		private final int[] generatedValues = new int[INCREASES_PER_TASK];

		private IncrementJob(GridDialect dialect, NextValueRequest nextValueRequest) {
			this.nextValueRequest = nextValueRequest;
			this.dialect = dialect;
		}

		@Override
		public void run() {
			for ( int i = 0; i < INCREASES_PER_TASK; i++ ) {
				recordValue( i, dialect.nextValue( nextValueRequest ) );
			}
		}

		private synchronized void recordValue(int i, Number sequenceValue) {
			generatedValues[i] = sequenceValue.intValue();
		}

		protected synchronized int[] retrieveAllGeneratedValues() {
			return generatedValues;
		}
	}
}
