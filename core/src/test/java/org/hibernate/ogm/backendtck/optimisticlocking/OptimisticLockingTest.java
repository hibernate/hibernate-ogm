/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.optimisticlocking;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.isA;
import static org.hibernate.ogm.utils.GridDialectType.COUCHDB;
import static org.hibernate.ogm.utils.GridDialectType.EHCACHE;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.impl.ForwardingGridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Test for detecting concurrent updates by dialects which support atomic find/update semantics or have their own
 * optimistic locking scheme.
 *
 * @author Gunnar Morling
 */
public class OptimisticLockingTest extends OgmTestCase {

	private static enum LatchAction {
		DECREASE_AND_WAIT, IGNORE
	};

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private ThreadFactory threadFactory;

	private final CountDownLatch deleteLatch = new CountDownLatch( 2 );

	@Before
	public void setupThreadFactory() {
		threadFactory = new ThreadFactoryBuilder().setNameFormat( "ogm-test-thread-%d" ).build();
	}

	@After
	public void cleanUp() {
		removePlanet();
		removePulsar();
	}

	/**
	 * This tests the "emulated" optimistic locking by means of re-reading the entity prior to updating it and comparing
	 * its version.
	 */
	@Test
	public void updatingEntityUsingOldVersionCausesException() throws Throwable {
		thrown.expect( StaleObjectStateException.class );

		persistPlanet();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// load the entity and update it
		Planet entity = (Planet) session.get( Planet.class, "planet-1" );
		entity.setName( "Uranus" );

		// update the entity in parallel...
		Future<?> future1 = updateInSeparateThread( Planet.class, "planet-1", "Mars", LatchAction.IGNORE );
		future1.get();

		// ... which will be detected by the re-read prior to the update
		commitTransactionAndPropagateExceptions( session, transaction );
	}

	/**
	 * This tests "real" optimistic locking by means of atomic find-and-modify semantics if supported by the datastore.
	 */
	@Test
	@SkipByGridDialect(
			value = { HASHMAP, INFINISPAN, EHCACHE, NEO4J, COUCHDB },
			comment = "Note that CouchDB has its own optimistic locking scheme, handled by the dialect itself."
	)
	public void updatingEntityUsingOldVersionCausesExceptionUsingAtomicFindAndUpdate() throws Throwable {
		thrown.expectCause( isA( StaleObjectStateException.class ) );

		persistPlanet();

		// for the first update, the test dialect waits a bit between read and write, so the second update will take
		// place in between, causing the exception
		Future<?> future1 = updateInSeparateThread( Planet.class, "planet-1", "Mars", LatchAction.DECREASE_AND_WAIT );
		Future<?> future2 = updateInSeparateThread( Planet.class, "planet-1", "Uranus", LatchAction.DECREASE_AND_WAIT );

		future2.get();
		future1.get();
	}

	/**
	 * This tests the "emulated" optimistic locking by means of re-reading the entity prior to removing it and comparing
	 * its version.
	 */
	@Test
	public void deletingEntityUsingOldVersionCausesException() throws Throwable {
		thrown.expect( StaleObjectStateException.class );

		persistPlanet();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// load the entity and delete it
		Planet entity = (Planet) session.get( Planet.class, "planet-1" );
		session.delete( entity );

		// update the entity in parallel...
		Future<?> future1 = updateInSeparateThread( Planet.class, "planet-1", "Mars", LatchAction.IGNORE );
		future1.get();

		// ... which will be detected by the re-read prior to the removal
		commitTransactionAndPropagateExceptions( session, transaction );
	}

	/**
	 * This tests "real" optimistic locking by means of atomic find-and-delete semantics if supported by the datastore.
	 */
	@Test
	@SkipByGridDialect(
			value = { HASHMAP, INFINISPAN, EHCACHE, NEO4J, COUCHDB },
			comment = "Note that CouchDB has its own optimistic locking scheme, handled by the dialect itself."
	)
	public void deletingEntityUsingOldVersionCausesExceptionUsingAtomicFindAndDelete() throws Throwable {
		thrown.expectCause( isA( StaleObjectStateException.class ) );

		persistPlanet();

		// for the delete, the test dialect waits a bit between read and delete, so the update will take place in
		// between, causing the exception
		Future<?> future1 = removePlanetInSeparateThread();
		Future<?> future2 = updateInSeparateThread( Planet.class, "planet-1", "Uranus", LatchAction.DECREASE_AND_WAIT );

		future2.get();
		future1.get();
	}

	/**
	 * This tests the "emulated" optimistic locking by means of re-reading the entity prior to updating it and comparing
	 * its version.
	 */
	@Test
	public void updatingEntityUsingOldEntityStateCausesException() throws Throwable {
		thrown.expect( StaleObjectStateException.class );

		persistPulsar();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// load the entity and update it
		Pulsar entity = (Pulsar) session.get( Pulsar.class, "pulsar-1" );
		entity.setName( "PSR J0537-6910" );

		// update the entity in parallel...
		Future<?> future1 = updateInSeparateThread( Pulsar.class, "pulsar-1", "PSR B1257+12", LatchAction.IGNORE );
		future1.get();

		// ... which will be detected by the re-read prior to the update
		commitTransactionAndPropagateExceptions( session, transaction );
	}

	/**
	 * This tests the "emulated" optimistic locking by means of re-reading the entity prior to deleting it and comparing
	 * its version.
	 */
	@Test
	public void deletingEntityUsingOldEntityStateCausesException() throws Throwable {
		thrown.expect( StaleObjectStateException.class );

		persistPulsar();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// load the entity and delete it
		Pulsar entity = (Pulsar) session.get( Pulsar.class, "pulsar-1" );
		session.delete( entity );

		// update the entity in parallel...
		Future<?> future1 = updateInSeparateThread( Pulsar.class, "pulsar-1", "PSR B1257+12", LatchAction.IGNORE );
		future1.get();

		// ... which will be detected by the re-read prior to the removal
		commitTransactionAndPropagateExceptions( session, transaction );
	}

	@Test
	public void updateToEmbeddedCollectionCausesVersionToBeIncreased() throws Throwable {
		Galaxy galaxy = persistGalaxy();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Galaxy entity = (Galaxy) session.get( Galaxy.class, galaxy.getId() );
		entity.getStars().add( new Star( "Algol" ) );

		transaction.commit();
		session.clear();

		session = openSession();
		transaction = session.beginTransaction();

		entity = (Galaxy) session.get( Galaxy.class, galaxy.getId() );
		assertThat( entity.getVersion() ).isEqualTo( 1 );
		assertThat( entity.getStars() ).hasSize( 3 );

		transaction.commit();
		session.close();
	}

	@Test
	public void mergingEntityUsingOldVersionCausesException() throws Throwable {
		thrown.expect( StaleObjectStateException.class );

		persistPlanet();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		// load the entity
		Planet entity = (Planet) session.get( Planet.class, "planet-1" );

		commitTransactionAndPropagateExceptions( session, transaction );

		// update the entity in parallel...
		Future<?> future1 = updateInSeparateThread( Planet.class, "planet-1", "Mars", LatchAction.IGNORE );
		future1.get();

		session = openSession();
		transaction = session.beginTransaction();

		// merging back the previously loaded version will cause an exception
		try {
			entity = (Planet) session.merge( entity );
		}
		finally {
			commitTransactionAndPropagateExceptions( session, transaction );
		}
	}

	private Future<?> updateInSeparateThread(final Class<? extends Nameable> type, final String id, final String newName, final LatchAction latchAction) throws Exception {
		return Executors.newSingleThreadExecutor().submit( new Runnable() {

			@Override
			public void run() {
				Session session = openSession();
				Transaction transaction = session.beginTransaction();

				// load the entity and update it
				Nameable entity = (Nameable) session.get( type, id );
				entity.setName( newName );

				if ( latchAction == LatchAction.DECREASE_AND_WAIT ) {
					countDownAndAwaitLatch();
				}

				transaction.commit();
				session.close();
			}
		} );
	}

	private Future<?> removePlanetInSeparateThread() throws Exception {
		return Executors.newSingleThreadExecutor( threadFactory ).submit( new Runnable() {

			@Override
			public void run() {
				Session session = openSession();
				Transaction transaction = session.beginTransaction();

				Planet entity;

				// load the entity and remove it
				entity = (Planet) session.get( Planet.class, "planet-1" );

				// the latch makes sure we have loaded the entity in both of the sessions before we're going to delete it
				countDownAndAwaitLatch();

				session.delete( entity );

				transaction.commit();
				session.close();
			}
		} );
	}

	private Planet persistPlanet() {
		Session session = openSession();

		session.beginTransaction();

		Planet planet = new Planet( "planet-1", "Pluto" );
		session.persist( planet );

		session.getTransaction().commit();
		session.close();

		return planet;
	}

	public void removePlanet() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Planet entity = (Planet) session.get( Planet.class, "planet-1" );
		if ( entity != null ) {
			session.delete( entity );
		}

		transaction.commit();
	}

	private Galaxy persistGalaxy() {
		Session session = openSession();

		session.beginTransaction();

		Galaxy milkyWay = new Galaxy( "galaxy-1", "Milky Way", new Star( "Sun" ), new Star( "Alpha Centauri" ) );
		session.persist( milkyWay );

		session.getTransaction().commit();
		session.close();

		return milkyWay;
	}

	private Pulsar persistPulsar() {
		Session session = openSession();

		session.beginTransaction();

		Pulsar pulsar = new Pulsar( "pulsar-1", "PSR 1919+21", 1.33 );
		session.persist( pulsar );

		session.getTransaction().commit();
		session.close();

		return pulsar;
	}

	public void removePulsar() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Pulsar entity = (Pulsar) session.get( Pulsar.class, "pulsar-1" );
		if ( entity != null ) {
			session.delete( entity );
		}

		transaction.commit();
	}

	private void commitTransactionAndPropagateExceptions(Session session, Transaction transaction) throws Exception {
		try {
			transaction.commit();
		}
		catch (Exception e) {
			transaction.rollback();
			throw e;
		}
		finally {
			session.close();
		}
	}

	private void countDownAndAwaitLatch() {
		deleteLatch.countDown();
		try {
			deleteLatch.await();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.getProperties().put( OgmProperties.GRID_DIALECT, TestDialect.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Planet.class, Galaxy.class, Pulsar.class };
	}

	@SuppressWarnings("serial")
	public static class TestDialect extends ForwardingGridDialect<Serializable> {

		public TestDialect(DatastoreProvider provider) {
			super( TestHelper.getCurrentGridDialect( provider ) );
		}

		@Override
		public boolean updateTupleWithOptimisticLock(EntityKey entityKey, Tuple oldVersion, Tuple tuple, TupleContext tupleContext) {
			if ( Thread.currentThread().getName().equals( "ogm-test-thread-0" ) ) {
				waitALittleBit();
			}

			return super.updateTupleWithOptimisticLock( entityKey, oldVersion, tuple, tupleContext );
		}

		@Override
		public boolean removeTupleWithOptimisticLock(EntityKey entityKey, Tuple oldVersion, TupleContext tupleContext) {
			if ( Thread.currentThread().getName().equals( "ogm-test-thread-0" ) ) {
				waitALittleBit();
			}

			return super.removeTupleWithOptimisticLock( entityKey, oldVersion, tupleContext );
		}

		private void waitALittleBit() {
			try {
				Thread.sleep( 1000 );
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
		}
	}
}
