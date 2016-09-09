/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.sequences;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.util.FixedBitSet;
import org.hibernate.ogm.backendtck.id.Actor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.impl.sequences.HotRodSequenceHandler;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteTestHelper;
import org.hibernate.ogm.datastore.infinispanremote.utils.RemoteHotRodServerRule;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.model.impl.DefaultIdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.utils.OgmSessionFactoryRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Stress test to verify the Sequence generation is generating unique numbers even
 * when under load by parallel requests.
 * If this test passes it does not guarantee that the implementation is correct;
 * if it fails there certainly is a problem.
 *
 * By default it's not running much of a stress test as we want the tests to give
 * quick feedback; Set the ogm.runFullStressTests System property to have it run
 * for several minutes.
 * Careful when enlarging constants for longer tests to also allow for more memory
 * as we need to track and compare all generated results at the end.
 *
 * @author Sanne Grinovero
 */
public class SequenceAtomicIncrementTest {

	private static boolean RUN_FULL_TESTS = Boolean.getBoolean( "ogm.runFullStressTests" );
	private static int PARALLEL_THREADS = Runtime.getRuntime().availableProcessors();
	private static int NUMBER_OF_TASKS = PARALLEL_THREADS * 3;
	private static int INCREASES_PER_TASK = RUN_FULL_TESTS ? 100_000 : 50;

	private static OgmSessionFactoryRule sessionFactoryHolder = new OgmSessionFactoryRule( Actor.class )
			.setConfigurationProperty( OgmProperties.DATASTORE_PROVIDER, "infinispan_remote" )
			.setConfigurationProperty( InfinispanRemoteProperties.CONFIGURATION_RESOURCE_NAME, "hotrod-client-testingconfiguration.properties" );

	private static RemoteHotRodServerRule hotRodServer = new RemoteHotRodServerRule();

	@Rule
	public RuleChain rules = RuleChain.outerRule( hotRodServer ).around( sessionFactoryHolder );

	@Test
	public void sequencerDoesNotGenerateDuplicates() throws InterruptedException {
		DefaultIdSourceKeyMetadata meta = DefaultIdSourceKeyMetadata.forSequence( "hibernate_sequences" );
		IdSourceKey idKey = IdSourceKey.forTable( meta, "actor_sequence_name" );
		NextValueRequest nextValueRequest = new NextValueRequest( idKey, 1, 0 );
		InfinispanRemoteDatastoreProvider provider = getProvider();
		HotRodSequenceHandler sequenceHandler = provider.getSequenceHandler();
		concurrentSequenceHandlerTest( sequenceHandler, nextValueRequest );
	}

	private void concurrentSequenceHandlerTest(HotRodSequenceHandler sequenceHandler, NextValueRequest nextValueRequest) throws InterruptedException {
		ExecutorService executorService = Executors.newWorkStealingPool( PARALLEL_THREADS );
		IncrementJob[] runJobs = new IncrementJob[NUMBER_OF_TASKS];
		System.out.println( "Starting stress tests on " + PARALLEL_THREADS + " Threads running " + NUMBER_OF_TASKS + " tasks" );
		// Prepare all jobs (quite a lot of array allocations):
		for ( int i = 0; i < NUMBER_OF_TASKS; i ++ ) {
			runJobs[i] = new IncrementJob( sequenceHandler, nextValueRequest );
		}
		// Start them, pretty much in parallel (not really, but we have a lot so they will eventually run in parallel):
		for ( int i = 0; i < NUMBER_OF_TASKS; i ++ ) {
			executorService.execute( runJobs[i] );
		}
		executorService.shutdown();
		executorService.awaitTermination( 10, TimeUnit.MINUTES );
		//Verify we got unique results:
		FixedBitSet bitset = new FixedBitSet( INCREASES_PER_TASK * NUMBER_OF_TASKS );
		for ( IncrementJob job : runJobs ) {
			int[] generatedValuesPerJob = job.retrieveAllGeneratedValues();
			for ( int generatedValue : generatedValuesPerJob ) {
				boolean previous = bitset.getAndSet( generatedValue );
				Assert.assertFalse( "Duplicate Key generated: " + generatedValue, previous );
			}
		}
	}

	public InfinispanRemoteDatastoreProvider getProvider() {
		InfinispanRemoteDatastoreProvider infinispanRemoteDatastoreProvider = InfinispanRemoteTestHelper.getProvider( sessionFactoryHolder.getOgmSessionFactory() );
		Assert.assertNotNull( infinispanRemoteDatastoreProvider );
		return infinispanRemoteDatastoreProvider;
	}

	private static class IncrementJob implements Runnable {

		private final HotRodSequenceHandler sequenceHandler;
		private final NextValueRequest nextValueRequest;
		//GuardedBy synchronization on IncrementJob.this :
		private final int[] generatedValues = new int[INCREASES_PER_TASK];
		private IncrementJob(HotRodSequenceHandler sequenceHandler, NextValueRequest nextValueRequest) {
			this.nextValueRequest = nextValueRequest;
			this.sequenceHandler = sequenceHandler;
		}

		@Override
		public void run() {
			for ( int i = 0; i < INCREASES_PER_TASK; i++ ) {
				recordValue( i, sequenceHandler.getSequenceValue( nextValueRequest ) );
			}
		}

		private synchronized void recordValue(int i, Number sequenceValue) {
			generatedValues[i] = sequenceValue.intValue();
		}

		private synchronized int[] retrieveAllGeneratedValues() {
			return generatedValues;
		}

	}

}
