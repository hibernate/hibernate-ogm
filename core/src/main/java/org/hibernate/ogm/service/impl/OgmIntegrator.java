/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.lang.reflect.Field;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.integrator.spi.ServiceContributingIntegrator;
import org.hibernate.jpa.event.spi.JpaIntegrator;
import org.hibernate.jpa.event.spi.jpa.CallbackRegistry;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.cfg.impl.Version;
import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.dialect.impl.BatchOperationsDelegator;
import org.hibernate.ogm.dialect.impl.ForwardingGridDialect;
import org.hibernate.ogm.dialect.impl.GridDialectInitiator;
import org.hibernate.ogm.dialect.impl.IdentityColumnAwareGridDialectInitiator;
import org.hibernate.ogm.dialect.impl.OgmDialectFactoryInitiator;
import org.hibernate.ogm.dialect.impl.OptimisticLockingAwareGridDialectInitiator;
import org.hibernate.ogm.dialect.impl.QueryableGridDialectInitiator;
import org.hibernate.ogm.dialect.impl.SessionFactoryLifecycleAwareDialectInitializer;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.jdbc.impl.OgmConnectionProviderInitiator;
import org.hibernate.ogm.jpa.impl.OgmPersisterClassResolverInitiator;
import org.hibernate.ogm.options.navigation.impl.OptionsServiceInitiator;
import org.hibernate.ogm.service.listener.impl.OgmDefaultMergeEventListener;
import org.hibernate.ogm.service.listener.impl.OgmDefaultPersistEventListener;
import org.hibernate.ogm.service.listener.impl.OgmDefaultPersistOnFlushEventListener;
import org.hibernate.ogm.service.listener.impl.OgmDefaultReplicateEventListener;
import org.hibernate.ogm.service.listener.impl.OgmDefaultSaveEventListener;
import org.hibernate.ogm.service.listener.impl.OgmDefaultSaveOrUpdateEventListener;
import org.hibernate.ogm.service.listener.impl.OgmDefaultUpdateEventListener;
import org.hibernate.ogm.service.listener.impl.OgmJpaMergeEventListener;
import org.hibernate.ogm.service.listener.impl.OgmJpaPersistEventListener;
import org.hibernate.ogm.service.listener.impl.OgmJpaPersistOnFlushEventListener;
import org.hibernate.ogm.service.listener.impl.OgmJpaSaveEventListener;
import org.hibernate.ogm.service.listener.impl.OgmJpaSaveOrUpdateEventListener;
import org.hibernate.ogm.service.listener.impl.OgmPersistEventDuplicationStrategy;
import org.hibernate.ogm.transaction.impl.OgmJtaPlatformInitiator;
import org.hibernate.ogm.transaction.impl.OgmTransactionFactoryInitiator;
import org.hibernate.ogm.type.impl.TypeTranslatorInitiator;
import org.hibernate.ogm.util.impl.TemporaryWorkaround;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Apply required services changes to run Hibernate OGM atop Hibernate Core
 *
 * In particular, set or override OGM specific services.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class OgmIntegrator implements Integrator, ServiceContributingIntegrator {

	@Override
	public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		doIntegrate( configuration, sessionFactory, serviceRegistry );
	}

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		doIntegrate( null, sessionFactory, serviceRegistry );
	}

	private void doIntegrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		if ( ! serviceRegistry.getService( ConfigurationService.class ).isOgmOn() ) {
			return;
		}
		Version.touch();

		sessionFactory.addObserver( new SchemaInitializingObserver( configuration ) );
		sessionFactory.addObserver( new SessionFactoryLifecycleAwareDialectInitializer() );
		attachBatchListenersIfRequired( serviceRegistry );

		attachPersistListener( serviceRegistry );
	}

	/**
	 * If the current dialect supports batching, register the required event listeners.
	 */
	private void attachBatchListenersIfRequired(SessionFactoryServiceRegistry serviceRegistry) {
		GridDialect gridDialect = serviceRegistry.getService( GridDialect.class );
		BatchOperationsDelegator batchDelegator = asBatchDelegatorOrNull( gridDialect );

		if ( batchDelegator != null ) {
			EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
			addListeners( eventListenerRegistry, batchDelegator );
		}
	}

	/**
	 * Registers OGM's persist event listeners.
	 */
	@TemporaryWorkaround("Only needed until HHH-9451 is fixed upstream")
	private void attachPersistListener(SessionFactoryServiceRegistry serviceRegistry) {
		EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

		JpaIntegrator integrator = getIntegrator( JpaIntegrator.class, serviceRegistry );
		CallbackRegistry callbackRegistry = integrator != null ? extractCallbackRegistry( integrator ) : null;

		eventListenerRegistry.addDuplicationStrategy( new OgmPersistEventDuplicationStrategy( callbackRegistry ) );

		eventListenerRegistry.getEventListenerGroup( EventType.MERGE ).appendListener( new OgmDefaultMergeEventListener() );
		eventListenerRegistry.getEventListenerGroup( EventType.PERSIST ).appendListener( new OgmDefaultPersistEventListener() );
		eventListenerRegistry.getEventListenerGroup( EventType.PERSIST_ONFLUSH ).appendListener( new OgmDefaultPersistOnFlushEventListener() );
		eventListenerRegistry.getEventListenerGroup( EventType.REPLICATE ).appendListener( new OgmDefaultReplicateEventListener() );
		eventListenerRegistry.getEventListenerGroup( EventType.SAVE ).appendListener( new OgmDefaultSaveEventListener() );
		eventListenerRegistry.getEventListenerGroup( EventType.SAVE_UPDATE ).appendListener( new OgmDefaultSaveOrUpdateEventListener() );
		eventListenerRegistry.getEventListenerGroup( EventType.UPDATE ).appendListener( new OgmDefaultUpdateEventListener() );
		eventListenerRegistry.getEventListenerGroup( EventType.MERGE ).appendListener( new OgmJpaMergeEventListener() );
		eventListenerRegistry.getEventListenerGroup( EventType.PERSIST ).appendListener( new OgmJpaPersistEventListener() );
		eventListenerRegistry.getEventListenerGroup( EventType.PERSIST_ONFLUSH ).appendListener( new OgmJpaPersistOnFlushEventListener() );
		eventListenerRegistry.getEventListenerGroup( EventType.SAVE ).appendListener( new OgmJpaSaveEventListener() );
		eventListenerRegistry.getEventListenerGroup( EventType.SAVE_UPDATE ).appendListener( new OgmJpaSaveOrUpdateEventListener() );
	}

	private <T extends Integrator> T getIntegrator(Class<T> integratorType, SessionFactoryServiceRegistry serviceRegistry) {
		Iterable<Integrator> integrators = serviceRegistry.getService( IntegratorService.class ).getIntegrators();

		for ( Integrator integrator : integrators ) {
			if ( integratorType.isInstance( integrator ) ) {
				return (T) integrator;
			}
		}

		return null;
	}

	private CallbackRegistry extractCallbackRegistry(JpaIntegrator integrator)  {
		try {
			Field registryField = JpaIntegrator.class.getDeclaredField( "callbackRegistry" );
			registryField.setAccessible( true );
			return (CallbackRegistry) registryField.get( integrator );
		}
		catch (Exception e) {
			throw new RuntimeException( "Can't extract callback registry", e );
		}
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
	}

	@Override
	public void prepareServices(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addInitiator( OgmSessionFactoryServiceRegistryFactoryInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( ConfigurationServiceInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmPersisterClassResolverInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmConnectionProviderInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmDialectFactoryInitiator.INSTANCE);
		serviceRegistryBuilder.addInitiator( OgmTransactionFactoryInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmJtaPlatformInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmJdbcServicesInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( DatastoreProviderInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OptionsServiceInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( TypeTranslatorInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( GridDialectInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( QueryableGridDialectInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( IdentityColumnAwareGridDialectInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OptimisticLockingAwareGridDialectInitiator.INSTANCE );
	}

	private BatchOperationsDelegator asBatchDelegatorOrNull(GridDialect gridDialect) {
		while ( gridDialect instanceof ForwardingGridDialect ) {
			if ( gridDialect instanceof BatchOperationsDelegator ) {
				return (BatchOperationsDelegator) gridDialect;
			}

			gridDialect = ( (ForwardingGridDialect<?>) gridDialect ).getGridDialect();
		}

		return null;
	}

	private void addListeners(EventListenerRegistry eventListenerRegistry, BatchOperationsDelegator gridDialect) {
		eventListenerRegistry.addDuplicationStrategy( new FlushBatchManagerEventListener.FlushDuplicationStrategy() );
		eventListenerRegistry.addDuplicationStrategy( new AutoFlushBatchManagerEventListener.AutoFlushDuplicationStrategy() );
		eventListenerRegistry.getEventListenerGroup( EventType.FLUSH ).appendListener( new FlushBatchManagerEventListener( gridDialect ) );
		eventListenerRegistry.getEventListenerGroup( EventType.AUTO_FLUSH ).appendListener( new AutoFlushBatchManagerEventListener( gridDialect ) );
	}
}
