/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.ServiceContributingIntegrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.cfg.impl.Version;
import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.dialect.impl.BatchOperationsDelegator;
import org.hibernate.ogm.dialect.impl.ForwardingGridDialect;
import org.hibernate.ogm.dialect.impl.GridDialectInitiator;
import org.hibernate.ogm.dialect.impl.OgmDialectFactoryInitiator;
import org.hibernate.ogm.dialect.impl.QueryableGridDialectInitiator;
import org.hibernate.ogm.dialect.impl.SessionFactoryLifecycleAwareDialectInitializer;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.jdbc.impl.OgmConnectionProviderInitiator;
import org.hibernate.ogm.jpa.impl.OgmPersisterClassResolverInitiator;
import org.hibernate.ogm.options.navigation.impl.OptionsServiceInitiator;
import org.hibernate.ogm.transaction.impl.OgmJtaPlatformInitiator;
import org.hibernate.ogm.transaction.impl.OgmTransactionFactoryInitiator;
import org.hibernate.ogm.type.impl.TypeTranslatorInitiator;
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
