/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.ServiceContributingIntegrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.cfg.impl.OgmNamingStrategy;
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
 * In particular:
 * - expose the OGM specific service(s)
 * - replace PersisterClassResolver
 * - replace ConnectionProvider
 * - replace DialectFactory
 * - replace (if not provided by the user) NamingStrategy
 *
 * Also enforce an OGM NamingStrategy
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmIntegrator implements Integrator, ServiceContributingIntegrator {

	@Override
	public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		if ( ! serviceRegistry.getService( ConfigurationService.class ).isOgmOn() ) {
			return;
		}
		Version.touch();
		configuration.setNamingStrategy( OgmNamingStrategy.INSTANCE );
	}

	@Override
	public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
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
