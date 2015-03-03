/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.exception;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.hibernate.StaleObjectStateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.exception.operation.spi.GridDialectOperation;
import org.hibernate.ogm.exception.operation.spi.InsertOrUpdateTuple;
import org.hibernate.ogm.exception.spi.ErrorHandler;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author Gunnar Morling
 */
public class ErrorSpiTest extends OgmTestCase {

	private static ExecutorService executor;

	@BeforeClass
	public static void setUpExecutor() {
		ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat( "ogm-test-thread-%d" ).build();
		executor = Executors.newSingleThreadExecutor( threadFactory );
	}

	@Before
	public void insertTestData() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		session.persist( new Shipment( "shipment-1", "INITIAL" ) );
		session.persist( new Shipment( "shipment-2", "INITIAL" ) );
		session.persist( new Shipment( "shipment-3", "INITIAL" ) );

		session.getTransaction().commit();
		session.close();
	}

	@After
	public void deleteTestDataAndResetErrorHandler() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		session.delete( session.get( Shipment.class, "shipment-1" ) );
		session.delete( session.get( Shipment.class, "shipment-2" ) );
		session.delete( session.get( Shipment.class, "shipment-3" ) );

		session.getTransaction().commit();
		session.close();

		MyErrorHandler.INSTANCE.clear();
	}

	@Test
	public void errorHandlerReceivesAppliedOperationsOfSameFlush() throws Exception {
		OgmSession session = openSession();
		session.getTransaction().begin();

		try {
			Shipment loadedShipment1 = (Shipment) session.get( Shipment.class, "shipment-1" );
			Shipment loadedShipment2 = (Shipment) session.get( Shipment.class, "shipment-2" );

			// do an update in parallel and wait until its done
			Future<?> future = updateShipmentInConcurrentThread( "shipment-2", "PROCESSING" );
			future.get();

			loadedShipment1.setState( "PROCESSING" );
			loadedShipment2.setState( "PROCESSING" );

			session.getTransaction().commit();

			fail( "expected exception was not raised" );
		}
		catch (StaleObjectStateException sose) {
			// Expected
		}
		finally {
			session.getTransaction().rollback();
			session.close();
		}

		// The update to shipment-1 is expected to be applied at the time that the update to shipment-2 causes the
		// rollback
		assertThat( MyErrorHandler.INSTANCE.getAppliedOperations() ).hasSize( 1 );
		List<GridDialectOperation> appliedOperations = MyErrorHandler.INSTANCE.getAppliedOperations().iterator().next();

		assertThat( appliedOperations ).hasSize( 1 );
		assertThat( appliedOperations.get( 0 ) ).isInstanceOf( InsertOrUpdateTuple.class );

		InsertOrUpdateTuple appliedOperation = appliedOperations.get( 0 ).as( InsertOrUpdateTuple.class );
		assertThat( appliedOperation.getEntityKey().getTable() ).isEqualTo( "Shipment" );
		assertThat( appliedOperation.getEntityKey().getColumnValues() ).isEqualTo( new Object[] { "shipment-1" } );
	}

//	TODO enable once we have "local" TX so we can test several concurrent TX with different data stores

//	@Test
//	public void AppliedOperationsPassedToErrorHandlerAreSeparatedByTransaction() throws Exception {
//		OgmSession sessionA = openSession();
//		sessionA.getTransaction().begin();
//
//		OgmSession sessionB = openSession();
//		sessionB.getTransaction().begin();
//
//		try {
//			Shipment loadedShipment1A = (Shipment) sessionA.get( Shipment.class, "shipment-1" );
//			Shipment loadedShipment2B = (Shipment) sessionB.get( Shipment.class, "shipment-2" );
//			Shipment loadedShipment3B = (Shipment) sessionB.get( Shipment.class, "shipment-3" );
//
//			// do an update in parallel and wait until its done
//			Future<?> future = updateShipmentInConcurrentThread( "shipment-3", "PROCESSING" );
//			future.get();
//
//			loadedShipment1A.setState( "PROCESSING" );
//			sessionA.flush();
//
//			loadedShipment2B.setState( "PROCESSING" );
//			loadedShipment3B.setState( "PROCESSING" );
//
//			sessionA.getTransaction().commit();
//			sessionB.getTransaction().commit();
//
//			fail( "expected exception was not raised" );
//		}
//		catch (StaleObjectStateException sose) {
//			sose.printStackTrace();
//			// Expected
//		}
//		finally {
//			sessionB.getTransaction().rollback();
//
//			sessionA.close();
//			sessionB.close();
//		}
//
//		// The update to shipment-1 is expected to be applied at the time that the update to shipment-2 causes the
//		// rollback
//		assertThat( MyErrorHandler.INSTANCE.getAppliedOperations() ).hasSize( 1 );
//		List<GridDialectOperation> appliedOperations = MyErrorHandler.INSTANCE.getAppliedOperations().iterator().next();
//
//		assertThat( appliedOperations ).hasSize( 1 );
//		assertThat( appliedOperations.get( 0 ) ).isInstanceOf( InsertOrUpdateTuple.class );
//
//		InsertOrUpdateTuple appliedOperation = appliedOperations.get( 0 ).as( InsertOrUpdateTuple.class );
//		assertThat( appliedOperation.getEntityKey().getTable() ).isEqualTo( "Shipment" );
//		assertThat( appliedOperation.getEntityKey().getColumnValues() ).isEqualTo( new Object[] { "shipment-1" } );
//	}

	private Future<?> updateShipmentInConcurrentThread(final String id, final String newState) {
		return executor.submit( new Runnable() {

			@Override
			public void run() {
				OgmSession session = openSession();
				session.getTransaction().begin();

				Shipment shipment = (Shipment) session.get( Shipment.class, id );
				shipment.setState( newState );

				session.getTransaction().commit();
				session.close();
			}

		} );
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.getProperties().put( OgmProperties.ERROR_HANDLER, MyErrorHandler.INSTANCE );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Shipment.class };
	}

	private static class MyErrorHandler implements ErrorHandler {

		static MyErrorHandler INSTANCE = new MyErrorHandler();

		private final Set<List<GridDialectOperation>> appliedOperations = new HashSet<>();

		@Override
		public void onRollback(RollbackContext context) {
			appliedOperations.add( context.getAppliedOperations() );
		}

		public Set<List<GridDialectOperation>> getAppliedOperations() {
			return appliedOperations;
		}

		public void clear() {
			appliedOperations.clear();
		}
	}
}
