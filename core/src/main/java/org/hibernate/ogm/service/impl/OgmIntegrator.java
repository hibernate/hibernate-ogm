/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.integrator.spi.ServiceContributingIntegrator;
import org.hibernate.jpa.event.spi.JpaIntegrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.impl.InternalProperties;
import org.hibernate.ogm.cfg.impl.Version;
import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManager;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagerInitiator;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingAutoFlushEventListener;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingAutoFlushEventListener.EventContextManagingAutoFlushEventListenerDuplicationStrategy;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingFlushEventListener;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingFlushEventListener.EventContextManagingFlushEventListenerDuplicationStrategy;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingPersistEventListener;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingPersistEventListener.EventContextManagingPersistEventListenerDuplicationStrategy;
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
import org.hibernate.ogm.transaction.impl.OgmJtaPlatformInitiator;
import org.hibernate.ogm.transaction.impl.OgmTransactionFactoryInitiator;
import org.hibernate.ogm.type.impl.TypeTranslatorInitiator;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Apply required services changes to run Hibernate OGM atop Hibernate ORM by setting OGM specific services and overriding
 * existing ORM services.
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

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
	}

	@Override
	public void prepareServices(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		if ( !isOgmUsed( serviceRegistryBuilder.getSettings() ) ) {
			return;
		}
		serviceRegistryBuilder.addInitiator( OgmSessionFactoryServiceRegistryFactoryInitiator.INSTANCE );
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
		serviceRegistryBuilder.addInitiator( EventContextManagerInitiator.INSTANCE );
	}

	private void doIntegrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		if ( !isOgmUsed( configuration.getProperties() ) ) {
			return;
		}
		Version.touch();

		sessionFactory.addObserver( new SchemaDefiningObserver( configuration ) );
		sessionFactory.addObserver( new SessionFactoryLifecycleAwareDialectInitializer() );

		attachBatchListenersIfRequired( serviceRegistry );
		attachEventContextManagingListenersIfRequired( configuration, serviceRegistry );
	}

	private boolean isOgmUsed(Map properties) {
		// Integrator are discovered via the Java ServiceLoader mechanism and get executed independently of whether a
		// a user actually wants to use OGM. For this reason an internal property (OGM_ON) is set in OgmConfiguration
		// resp. HibernateOgmPersistence to indicate the actual bootstrapping of OGM. The required OGM settings
		// will only be applied if this flag is set when the callbacks of this integrator is called.
		return new ConfigurationPropertyReader( properties )
				.property( InternalProperties.OGM_ON, boolean.class )
				.withDefault( false )
				.getValue();
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

	private void attachEventContextManagingListenersIfRequired(Configuration configuration, SessionFactoryServiceRegistry serviceRegistry) {
		if ( !isEventContextRequired( configuration ) ) {
			return;
		}

		EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		EventContextManager stateManager = serviceRegistry.getService( EventContextManager.class );

		eventListenerRegistry.addDuplicationStrategy( EventContextManagingAutoFlushEventListenerDuplicationStrategy.INSTANCE );
		eventListenerRegistry.getEventListenerGroup( EventType.AUTO_FLUSH ).appendListener( new EventContextManagingAutoFlushEventListener( stateManager ) );

		eventListenerRegistry.addDuplicationStrategy( EventContextManagingFlushEventListenerDuplicationStrategy.INSTANCE );
		eventListenerRegistry.getEventListenerGroup( EventType.FLUSH ).appendListener( new EventContextManagingFlushEventListener( stateManager ) );

		if ( getIntegrator( JpaIntegrator.class, serviceRegistry ) != null ) {
			eventListenerRegistry.addDuplicationStrategy( EventContextManagingPersistEventListenerDuplicationStrategy.INSTANCE );
			eventListenerRegistry.getEventListenerGroup( EventType.PERSIST ).appendListener( new EventContextManagingPersistEventListener( stateManager ) );
		}
	}

	private boolean isEventContextRequired(Configuration configuration) {
		return configuration.getProperties().get( OgmProperties.ERROR_HANDLER ) != null;
	}

	@SuppressWarnings( "unchecked" )
	private <T extends Integrator> T getIntegrator(Class<T> integratorType, SessionFactoryServiceRegistry serviceRegistry) {
		Iterable<Integrator> integrators = serviceRegistry.getService( IntegratorService.class ).getIntegrators();

		for ( Integrator integrator : integrators ) {
			if ( integratorType.isInstance( integrator ) ) {
				return (T) integrator;
			}
		}

		return null;
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
