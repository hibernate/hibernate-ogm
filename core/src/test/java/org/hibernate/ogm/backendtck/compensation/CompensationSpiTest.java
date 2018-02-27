/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.compensation;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.persistence.OptimisticLockException;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.compensation.ErrorHandler.FailedGridDialectOperationContext;
import org.hibernate.ogm.compensation.ErrorHandler.RollbackContext;
import org.hibernate.ogm.compensation.operation.CreateTupleWithKey;
import org.hibernate.ogm.compensation.operation.ExecuteBatch;
import org.hibernate.ogm.compensation.operation.GridDialectOperation;
import org.hibernate.ogm.compensation.operation.InsertOrUpdateTuple;
import org.hibernate.ogm.compensation.operation.UpdateTupleWithOptimisticLock;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.optimisticlock.spi.OptimisticLockingAwareGridDialect;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.dialect.spi.TupleAlreadyExistsException;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Tests around the error compensation SPI.
 *
 * @author Gunnar Morling
 */
public class CompensationSpiTest extends OgmTestCase {

	private static ExecutorService executor;

	@BeforeClass
	public static void setUpExecutor() {
		ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat( "ogm-test-thread-%d" ).build();
		executor = Executors.newSingleThreadExecutor( threadFactory );
	}

	@Test
	public void onRollbackPresentsAppliedInsertOperations() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given two inserted records
		session.persist( new Shipment( "shipment-1", "INITIAL" ) );
		session.persist( new Shipment( "shipment-2", "INITIAL" ) );
		session.flush();
		session.clear();

		try {
			// when provoking a duplicate-key exception
			session.persist( new Shipment( "shipment-1", "INITIAL" ) );

			session.getTransaction().commit();
		}
		catch (Exception e) {
			rollbackTransactionIfActive( session.getTransaction() );
		}

		// then expect the ops for inserting the two records
		Iterator<RollbackContext> onRollbackInvocations = InvocationTrackingHandler.INSTANCE.getOnRollbackInvocations().iterator();
		Iterator<GridDialectOperation> appliedOperations = onRollbackInvocations.next().getAppliedGridDialectOperations().iterator();
		assertThat( onRollbackInvocations.hasNext() ).isFalse();

		if ( currentDialectHasFacet( BatchableGridDialect.class ) ||
				currentDialectHasFacet( GroupingByEntityDialect.class ) ) {
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			GridDialectOperation operation = appliedOperations.next();
			assertThat( operation ).isInstanceOf( ExecuteBatch.class );

			ExecuteBatch batch = operation.as( ExecuteBatch.class );
			Iterator<GridDialectOperation> batchedOperations = batch.getOperations().iterator();
			assertThat( batchedOperations.next() ).isInstanceOf( InsertOrUpdateTuple.class );
			assertThat( batchedOperations.next() ).isInstanceOf( InsertOrUpdateTuple.class );
			assertThat( batchedOperations.hasNext() ).isFalse();
		}
		else {
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			assertThat( appliedOperations.next() ).isInstanceOf( InsertOrUpdateTuple.class );
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			assertThat( appliedOperations.next() ).isInstanceOf( InsertOrUpdateTuple.class );
		}

		// If LOOK_UP is used for duplicate prevention, the duplicated id will be detected prior to the actual insert
		// itself; otherwise, the CreateTuple call will succeed, and only the insert call will fail
		if ( currentDialectUsesLookupDuplicatePreventionStrategy() ) {
			assertThat( appliedOperations.hasNext() ).isFalse();
		}
		else {
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
		}

		session.close();
	}

	@Test
	public void onRollbackPresentsAppliedInsertOperationsForSave() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given two inserted records
		session.persist( new Shipment( "shipment-1", "INITIAL" ) );
		session.persist( new Shipment( "shipment-2", "INITIAL" ) );
		session.flush();
		session.clear();

		try {
			// when provoking a duplicate-key exception
			session.save( new Shipment( "shipment-1", "INITIAL" ) );
			session.getTransaction().commit();
		}
		catch (Exception e) {
			rollbackTransactionIfActive( session.getTransaction() );
		}

		// then expect the ops for inserting the two records
		Iterator<RollbackContext> onRollbackInvocations = InvocationTrackingHandler.INSTANCE.getOnRollbackInvocations().iterator();
		Iterator<GridDialectOperation> appliedOperations = onRollbackInvocations.next().getAppliedGridDialectOperations().iterator();
		assertThat( onRollbackInvocations.hasNext() ).isFalse();

		if ( currentDialectHasFacet( BatchableGridDialect.class )
				|| currentDialectHasFacet( GroupingByEntityDialect.class ) ) {
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			assertThat( appliedOperations.next() ).isInstanceOf( ExecuteBatch.class );
		}
		else {
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			assertThat( appliedOperations.next() ).isInstanceOf( InsertOrUpdateTuple.class );
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			assertThat( appliedOperations.next() ).isInstanceOf( InsertOrUpdateTuple.class );
		}

		// If LOOK_UP is used for duplicate prevention, the duplicated id will be detected prior to the actual insert
		// itself; otherwise, the CreateTuple call will succeed, and only the insert call will fail
		if ( currentDialectUsesLookupDuplicatePreventionStrategy() ) {
			assertThat( appliedOperations.hasNext() ).isFalse();
		}
		else {
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
		}

		session.close();
	}

	@Test
	public void onRollbackPresentsAppliedUpdateOperations() throws Exception {
		OgmSession session = openSession();
		session.getTransaction().begin();

		Shipment shipment1 = new Shipment( "shipment-1", "INITIAL" );
		session.persist( shipment1 );

		Shipment shipment2 = new Shipment( "shipment-2", "INITIAL" );
		session.persist( shipment2 );

		session.getTransaction().commit();
		session.clear();
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
		catch (OptimisticLockException sose) {
			// Expected
		}
		finally {
			rollbackTransactionIfActive( session.getTransaction() );
			session.close();
		}

		Iterator<RollbackContext> onRollbackInvocations = InvocationTrackingHandler.INSTANCE.getOnRollbackInvocations().iterator();
		Iterator<GridDialectOperation> appliedOperations = onRollbackInvocations.next().getAppliedGridDialectOperations().iterator();
		assertThat( onRollbackInvocations.hasNext() ).isFalse();

		if ( currentDialectHasFacet( OptimisticLockingAwareGridDialect.class ) ) {
			GridDialectOperation appliedOperation = appliedOperations.next();
			assertThat( appliedOperation ).isInstanceOf( UpdateTupleWithOptimisticLock.class );
			UpdateTupleWithOptimisticLock updateTupleWithOptimisticLock = appliedOperation.as( UpdateTupleWithOptimisticLock.class );
			assertThat( updateTupleWithOptimisticLock.getEntityKey().getTable() ).isEqualTo( "Shipment" );
			assertThat( updateTupleWithOptimisticLock.getEntityKey().getColumnValues() ).isEqualTo( new Object[] { "shipment-1" } );
		}
		else if ( currentDialectHasFacet( GroupingByEntityDialect.class ) ) {
			GridDialectOperation operation = appliedOperations.next();
			assertThat( operation ).isInstanceOf( ExecuteBatch.class );
			ExecuteBatch batch = operation.as( ExecuteBatch.class );
			Iterator<GridDialectOperation> batchedOperations = batch.getOperations().iterator();
			InsertOrUpdateTuple insertOrUpdate = batchedOperations.next().as( InsertOrUpdateTuple.class );
			assertThat( insertOrUpdate.getEntityKey().getTable() ).isEqualTo( "Shipment" );
			assertThat( insertOrUpdate.getEntityKey().getColumnValues() ).isEqualTo( new Object[] { "shipment-1" } );
			assertThat( batchedOperations.hasNext() ).isFalse();
		}
		else {
			GridDialectOperation appliedOperation = appliedOperations.next();
			assertThat( appliedOperation ).isInstanceOf( InsertOrUpdateTuple.class );
			InsertOrUpdateTuple insertOrUpdate = appliedOperation.as( InsertOrUpdateTuple.class );
			assertThat( insertOrUpdate.getEntityKey().getTable() ).isEqualTo( "Shipment" );
			assertThat( insertOrUpdate.getEntityKey().getColumnValues() ).isEqualTo( new Object[] { "shipment-1" } );
		}

		assertThat( appliedOperations.hasNext() ).isFalse();
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.NEO4J_EMBEDDED, GridDialectType.NEO4J_REMOTE, GridDialectType.INFINISPAN },
			comment = "Can use parallel local TX not with JTA"
	)
	public void appliedOperationsPassedToErrorHandlerAreSeparatedByTransaction() throws Exception {
		OgmSession session = openSession();
		session.getTransaction().begin();

		session.persist( new Shipment( "shipment-1", "INITIAL" ) );
		session.persist( new Shipment( "shipment-2", "INITIAL" ) );
		session.persist( new Shipment( "shipment-3", "INITIAL" ) );

		session.getTransaction().commit();
		session.close();


		OgmSession sessionA = openSession();
		sessionA.getTransaction().begin();

		OgmSession sessionB = openSession();
		sessionB.getTransaction().begin();

		try {
			Shipment loadedShipment1A = (Shipment) sessionA.get( Shipment.class, "shipment-1" );
			Shipment loadedShipment2B = (Shipment) sessionB.get( Shipment.class, "shipment-2" );
			Shipment loadedShipment3B = (Shipment) sessionB.get( Shipment.class, "shipment-3" );

			// do an update in parallel which will cause the rollback of TX B and wait until its done
			Future<?> future = updateShipmentInConcurrentThread( "shipment-3", "PROCESSING" );
			future.get();

			loadedShipment1A.setState( "PROCESSING" );
			sessionA.flush();

			loadedShipment2B.setState( "PROCESSING" );
			loadedShipment3B.setState( "PROCESSING" );

			sessionA.getTransaction().commit();
			sessionB.getTransaction().commit();

			fail( "expected exception was not raised" );
		}
		catch (OptimisticLockException sose) {
			// Expected
		}
		finally {
			rollbackTransactionIfActive( sessionA.getTransaction() );
			rollbackTransactionIfActive( sessionB.getTransaction() );

			sessionA.close();
			sessionB.close();
		}

		// The update to shipment-1 is done by TX A, so only the update to shipment-2 is expected in the applied ops by TX B
		// upon rollback due to the failure of the update to shipment-3
		Iterator<RollbackContext> onRollbackInvocations = InvocationTrackingHandler.INSTANCE.getOnRollbackInvocations().iterator();
		Iterator<GridDialectOperation> appliedOperations = onRollbackInvocations.next().getAppliedGridDialectOperations().iterator();
		assertThat( onRollbackInvocations.hasNext() ).isFalse();

		if ( currentDialectHasFacet( OptimisticLockingAwareGridDialect.class ) ) {
			GridDialectOperation appliedOperation = appliedOperations.next();
			assertThat( appliedOperation ).isInstanceOf( UpdateTupleWithOptimisticLock.class );
			UpdateTupleWithOptimisticLock updateTupleWithOptimisticLock = appliedOperation.as( UpdateTupleWithOptimisticLock.class );
			assertThat( updateTupleWithOptimisticLock.getEntityKey().getTable() ).isEqualTo( "Shipment" );
			assertThat( updateTupleWithOptimisticLock.getEntityKey().getColumnValues() ).isEqualTo( new Object[] { "shipment-2" } );
		}
		else if ( currentDialectHasFacet( GroupingByEntityDialect.class ) ) {
			GridDialectOperation operation = appliedOperations.next();
			assertThat( operation ).isInstanceOf( ExecuteBatch.class );
			ExecuteBatch batch = operation.as( ExecuteBatch.class );
			Iterator<GridDialectOperation> batchedOperations = batch.getOperations().iterator();
			InsertOrUpdateTuple insertOrUpdate = batchedOperations.next().as( InsertOrUpdateTuple.class );
			assertThat( insertOrUpdate.getEntityKey().getTable() ).isEqualTo( "Shipment" );
			assertThat( insertOrUpdate.getEntityKey().getColumnValues() ).isEqualTo( new Object[] { "shipment-2" } );
			assertThat( batchedOperations.hasNext() ).isFalse();
		}
		else {
			GridDialectOperation appliedOperation = appliedOperations.next();
			assertThat( appliedOperation ).isInstanceOf( InsertOrUpdateTuple.class );
			InsertOrUpdateTuple insertOrUpdate = appliedOperation.as( InsertOrUpdateTuple.class );
			assertThat( insertOrUpdate.getEntityKey().getTable() ).isEqualTo( "Shipment" );
			assertThat( insertOrUpdate.getEntityKey().getColumnValues() ).isEqualTo( new Object[] { "shipment-2" } );
		}
	}

	@Test
	public void onFailedOperationPresentsFailedAndAppliedOperationsAndException() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given two inserted records
		session.persist( new Shipment( "shipment-1", "INITIAL" ) );
		session.persist( new Shipment( "shipment-2", "INITIAL" ) );
		session.flush();
		session.clear();

		try {
			// when provoking a duplicate-key exception
			session.persist( new Shipment( "shipment-1", "INITIAL" ) );
			session.getTransaction().commit();
			fail( "Expected exception was not raised" );
		}
		catch (Exception e) {
			rollbackTransactionIfActive( session.getTransaction() );
		}

		Iterator<FailedGridDialectOperationContext> onFailedOperationInvocations = InvocationTrackingHandler.INSTANCE.getOnFailedOperationInvocations().iterator();
		FailedGridDialectOperationContext invocation = onFailedOperationInvocations.next();
		assertThat( onFailedOperationInvocations.hasNext() ).isFalse();

		// then expect the failed op
		if ( (currentDialectHasFacet( BatchableGridDialect.class ) || currentDialectHasFacet( GroupingByEntityDialect.class )) &&
				!currentDialectUsesLookupDuplicatePreventionStrategy() ) {
			assertThat( invocation.getFailedOperation() ).isInstanceOf( ExecuteBatch.class );
		}
		else {
			assertThat( invocation.getFailedOperation() ).isInstanceOf( InsertOrUpdateTuple.class );
		}

		// and the exception
		assertThat( invocation.getException() ).isExactlyInstanceOf( TupleAlreadyExistsException.class );

		// and the applied ops
		Iterator<GridDialectOperation> appliedOperations = invocation.getAppliedGridDialectOperations().iterator();

		if ( currentDialectHasFacet( BatchableGridDialect.class ) ||
				currentDialectHasFacet( GroupingByEntityDialect.class ) ) {
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			GridDialectOperation operation = appliedOperations.next();
			assertThat( operation ).isInstanceOf( ExecuteBatch.class );

			ExecuteBatch batch = operation.as( ExecuteBatch.class );
			Iterator<GridDialectOperation> batchedOperations = batch.getOperations().iterator();
			assertThat( batchedOperations.next() ).isInstanceOf( InsertOrUpdateTuple.class );
			assertThat( batchedOperations.next() ).isInstanceOf( InsertOrUpdateTuple.class );
			assertThat( batchedOperations.hasNext() ).isFalse();
		}
		else {
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			assertThat( appliedOperations.next() ).isInstanceOf( InsertOrUpdateTuple.class );
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
			assertThat( appliedOperations.next() ).isInstanceOf( InsertOrUpdateTuple.class );
		}

		if ( currentDialectUsesLookupDuplicatePreventionStrategy() ) {
			assertThat( appliedOperations.hasNext() ).isFalse();
		}
		else {
			assertThat( appliedOperations.next() ).isInstanceOf( CreateTupleWithKey.class );
		}

		session.close();
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.NEO4J_EMBEDDED, GridDialectType.NEO4J_REMOTE },
			comment = "Transaction cannot be committed when continuing after an exception "
	)
	public void subsequentOperationsArePerformedForErrorHandlingStrategyContinue() {
		OgmSessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory(
				Collections.<String, Object>singletonMap( OgmProperties.ERROR_HANDLER, ContinuingErrorHandler.INSTANCE ),
				getAnnotatedClasses()
		);

		OgmSession session = sessionFactory.openSession();
		session.getTransaction().begin();

		session.persist( new Shipment( "shipment-1", "INITIAL" ) );
		session.persist( new Shipment( "shipment-2", "INITIAL" ) );

		// TODO: without flush/clear ORM itself would detect the duplicate entity; should we relay this exception to the
		// handler prior to rollback?
		session.flush();
		session.clear();

		// when provoking a duplicate-key exception
		session.persist( new Shipment( "shipment-1", "INITIAL" ) );

		// TODO without the flush we'll batch this and the next insert; we cannot continue with remaining elements of a batch
		session.flush();

		session.persist( new Shipment( "shipment-3", "INITIAL" ) );

		session.getTransaction().commit();
		session.close();

		session = sessionFactory.openSession();
		session.getTransaction().begin();

		// then expect all previously and subsequent operations applied
		Shipment loadedShipment = (Shipment) session.get( Shipment.class, "shipment-1" );
		assertThat( loadedShipment ).isNotNull();

		loadedShipment = (Shipment) session.get( Shipment.class, "shipment-2" );
		assertThat( loadedShipment ).isNotNull();

		loadedShipment = (Shipment) session.get( Shipment.class, "shipment-3" );
		assertThat( loadedShipment ).isNotNull();

		session.getTransaction().commit();
		session.close();
	}

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

	@After
	public void deleteTestDataAndResetErrorHandler() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		Shipment shipment = (Shipment) session.get( Shipment.class, "shipment-1" );
		if ( shipment != null ) {
			session.delete( shipment );
		}

		shipment = (Shipment) session.get( Shipment.class, "shipment-2" );
		if ( shipment != null ) {
			session.delete( shipment );
		}

		shipment = (Shipment) session.get( Shipment.class, "shipment-3" );
		if ( shipment != null ) {
			session.delete( shipment );
		}

		shipment = (Shipment) session.get( Shipment.class, "shipment-4" );
		if ( shipment != null ) {
			session.delete( shipment );
		}

		session.getTransaction().commit();
		session.close();

		InvocationTrackingHandler.INSTANCE.clear();
	}

	@Override
	protected void configure(Map<String, Object> settings) {
		settings.put( OgmProperties.ERROR_HANDLER, InvocationTrackingHandler.INSTANCE );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Shipment.class };
	}

	private boolean currentDialectHasFacet(Class<? extends GridDialect> facet) {
		GridDialect gridDialect = getSessionFactory().getServiceRegistry().getService( GridDialect.class );
		return GridDialects.hasFacet( gridDialect, facet );
	}

	private boolean currentDialectUsesLookupDuplicatePreventionStrategy() {
		GridDialect gridDialect = getSessionFactory().getServiceRegistry().getService( GridDialect.class );
		DefaultEntityKeyMetadata ekm = new DefaultEntityKeyMetadata( "Shipment", new String[]{"id"} );

		return gridDialect.getDuplicateInsertPreventionStrategy( ekm ) == DuplicateInsertPreventionStrategy.LOOK_UP;
	}

	/**
	 * In JTA the failed commit attempt will have done the rollback already. The TX is NOT_ACTIVE in this case.
	 */
	private void rollbackTransactionIfActive(Transaction transaction) {
		if ( transaction.getStatus() == TransactionStatus.ACTIVE ) {
			transaction.rollback();
		}
	}
}
