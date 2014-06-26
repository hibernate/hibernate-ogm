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
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.ServiceContributingIntegrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.cfg.impl.Version;
import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.dialect.OgmDialectFactoryInitiator;
import org.hibernate.ogm.jdbc.OgmConnectionProviderInitiator;
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
	}
}
