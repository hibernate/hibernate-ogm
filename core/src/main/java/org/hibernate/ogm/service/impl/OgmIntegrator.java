/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.ogm.cfg.impl.Version;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManager;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingAutoFlushEventListener;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingAutoFlushEventListener.EventContextManagingAutoFlushEventListenerDuplicationStrategy;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingFlushEventListener;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingFlushEventListener.EventContextManagingFlushEventListenerDuplicationStrategy;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingPersistEventListener;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagingPersistEventListener.EventContextManagingPersistEventListenerDuplicationStrategy;
import org.hibernate.ogm.dialect.impl.SessionFactoryLifecycleAwareDialectInitializer;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Apply required services changes to run Hibernate OGM atop Hibernate ORM by setting OGM specific services and overriding
 * existing ORM services.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class OgmIntegrator implements Integrator {

	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		doIntegrate( metadata, sessionFactory, serviceRegistry );
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
	}

	private void doIntegrate(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		if ( ! serviceRegistry.getService( OgmConfigurationService.class ).isOgmEnabled() ) {
			return;
		}
		Version.touch();

		sessionFactory.addObserver( new SchemaDefiningObserver( metadata ) );
		sessionFactory.addObserver( new SessionFactoryLifecycleAwareDialectInitializer() );

		attachEventContextManagingListenersIfRequired( serviceRegistry );
	}

	private void attachEventContextManagingListenersIfRequired(SessionFactoryServiceRegistry serviceRegistry) {
		if ( !EventContextManager.isEventContextRequired( serviceRegistry ) ) {
			return;
		}

		EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
		EventContextManager stateManager = serviceRegistry.getService( EventContextManager.class );

		eventListenerRegistry.addDuplicationStrategy( EventContextManagingAutoFlushEventListenerDuplicationStrategy.INSTANCE );
		eventListenerRegistry.getEventListenerGroup( EventType.AUTO_FLUSH ).appendListener( new EventContextManagingAutoFlushEventListener( stateManager ) );

		eventListenerRegistry.addDuplicationStrategy( EventContextManagingFlushEventListenerDuplicationStrategy.INSTANCE );
		eventListenerRegistry.getEventListenerGroup( EventType.FLUSH ).appendListener( new EventContextManagingFlushEventListener( stateManager ) );

		eventListenerRegistry.addDuplicationStrategy( EventContextManagingPersistEventListenerDuplicationStrategy.INSTANCE );
		eventListenerRegistry.getEventListenerGroup( EventType.PERSIST ).appendListener( new EventContextManagingPersistEventListener( stateManager ) );
	}

}
