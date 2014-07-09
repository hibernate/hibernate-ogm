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
import org.hibernate.ogm.dialect.OgmDialectFactoryInitiator;
import org.hibernate.ogm.jdbc.OgmConnectionProviderInitiator;
import org.hibernate.ogm.jpa.impl.OgmPersisterClassResolverInitiator;
import org.hibernate.ogm.transaction.impl.OgmJtaPlatformInitiator;
import org.hibernate.ogm.transaction.impl.OgmTransactionFactoryInitiator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Apply required services changes to run Hibernate OGM atop Hibernate Core
 *
 * In particular, set or override OGM specific services.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmIntegrator implements Integrator, ServiceContributingIntegrator {

	@Override
	public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		doIntegrate( serviceRegistry );
	}

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		doIntegrate( serviceRegistry );
	}

	private void doIntegrate(SessionFactoryServiceRegistry serviceRegistry) {
		if ( ! serviceRegistry.getService( ConfigurationService.class ).isOgmOn() ) {
			return;
		}
		Version.touch();
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
	}
}
