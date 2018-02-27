/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.compensation;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.compensation.ErrorHandler.RollbackContext;
import org.hibernate.ogm.compensation.operation.CreateTupleWithKey;
import org.hibernate.ogm.compensation.operation.ExecuteBatch;
import org.hibernate.ogm.compensation.operation.GridDialectOperation;
import org.hibernate.ogm.compensation.operation.InsertOrUpdateTuple;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.spi.GroupingByEntityDialect;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.PackagingRule;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for using the compensation SPI with JPA.
 *
 * @author Gunnar Morling
 *
 */
public class CompensationSpiJpaTest  extends OgmJpaTestCase {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/transaction-type-jta.xml", Shipment.class );

	@Test
	@SkipByGridDialect(value = { GridDialectType.MONGODB, GridDialectType.INFINISPAN_REMOTE },
		comment = "MongoDB and Infinispan Remote tests runs w/o transaction manager")
	public void onRollbackTriggeredThroughJtaPresentsAppliedInsertOperations() throws Exception {
		Map<String, Object> settings = new HashMap<>();
		settings.putAll( TestHelper.getDefaultTestSettings() );
		settings.put( OgmProperties.ERROR_HANDLER, InvocationTrackingHandler.INSTANCE );

		EntityManagerFactory emf = Persistence.createEntityManagerFactory( "transaction-type-jta", settings );

		TransactionManager transactionManager = getTransactionManager( emf );

		transactionManager.begin();
		EntityManager em = emf.createEntityManager();

		// given two inserted records
		em.persist( new Shipment( "shipment-1", "INITIAL" ) );
		em.persist( new Shipment( "shipment-2", "INITIAL" ) );

		em.flush();
		em.clear();

		try {
			// when provoking a duplicate-key exception
			em.persist( new Shipment( "shipment-1", "INITIAL" ) );
			transactionManager.commit();
			fail( "Expected exception was not raised" );
		}
		catch (Exception e) {
			// Nothing to do
		}

		// then expect the ops for inserting the two records
		Iterator<RollbackContext> onRollbackInvocations = InvocationTrackingHandler.INSTANCE.getOnRollbackInvocations().iterator();
		Iterator<GridDialectOperation> appliedOperations = onRollbackInvocations.next().getAppliedGridDialectOperations().iterator();
		assertThat( onRollbackInvocations.hasNext() ).isFalse();

		if ( currentDialectHasFacet( BatchableGridDialect.class ) || currentDialectHasFacet( GroupingByEntityDialect.class ) ) {
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

		transactionManager.begin();
		em.joinTransaction();

		Shipment shipment = em.find( Shipment.class, "shipment-1" );
		if ( shipment != null ) {
			em.remove( shipment );
		}

		shipment = em.find( Shipment.class, "shipment-2" );
		if ( shipment != null ) {
			em.remove( shipment );
		}

		transactionManager.commit();

		em.close();
	}

	@Test
	public void onRollbackTriggeredThroughManualRollbackPresentsAppliedInsertOperations() throws Exception {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		// given two inserted records
		em.persist( new Shipment( "shipment-1", "INITIAL" ) );
		em.persist( new Shipment( "shipment-2", "INITIAL" ) );

		em.flush();
		em.clear();

		try {
			// when provoking a duplicate-key exception
			em.persist( new Shipment( "shipment-1", "INITIAL" ) );
			em.getTransaction().commit();
			fail( "Expected exception was not raised" );
		}
		catch (Exception e) {
			// Nothing to do
		}

		// then expect the ops for inserting the two records
		Iterator<RollbackContext> onRollbackInvocations = InvocationTrackingHandler.INSTANCE.getOnRollbackInvocations().iterator();
		Iterator<GridDialectOperation> appliedOperations = onRollbackInvocations.next().getAppliedGridDialectOperations().iterator();
		assertThat( onRollbackInvocations.hasNext() ).isFalse();

		if ( currentDialectHasFacet( BatchableGridDialect.class ) || currentDialectHasFacet( GroupingByEntityDialect.class ) ) {
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

		em.clear();
		em.getTransaction().begin();

		Shipment shipment = em.find( Shipment.class, "shipment-1" );
		if ( shipment != null ) {
			em.remove( shipment );
		}

		shipment = em.find( Shipment.class, "shipment-2" );
		if ( shipment != null ) {
			em.remove( shipment );
		}

		em.getTransaction().commit();
		em.close();
	}

	@After
	public void resetErrorHandler() throws Exception {
		InvocationTrackingHandler.INSTANCE.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Shipment.class };
	}

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		info.getProperties().put( OgmProperties.ERROR_HANDLER, InvocationTrackingHandler.INSTANCE );
	}

	private boolean currentDialectHasFacet(Class<? extends GridDialect> facet) {
		SessionFactoryImplementor sfi = (SessionFactoryImplementor) getFactory();
		GridDialect gridDialect = sfi.getServiceRegistry().getService( GridDialect.class );
		return GridDialects.hasFacet( gridDialect, facet );
	}

	private boolean currentDialectUsesLookupDuplicatePreventionStrategy() {
		SessionFactoryImplementor sfi = (SessionFactoryImplementor) getFactory();
		GridDialect gridDialect = sfi.getServiceRegistry().getService( GridDialect.class );
		DefaultEntityKeyMetadata ekm = new DefaultEntityKeyMetadata( "Shipment", new String[]{"id"} );

		return gridDialect.getDuplicateInsertPreventionStrategy( ekm ) == DuplicateInsertPreventionStrategy.LOOK_UP;
	}

}
