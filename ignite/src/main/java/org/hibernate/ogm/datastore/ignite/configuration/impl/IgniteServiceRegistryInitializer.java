/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.configuration.impl;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.ogm.datastore.ignite.jpa.impl.IgniteOgmPersisterClassResolver;
import org.hibernate.ogm.datastore.ignite.query.impl.IgniteQueryTranslatorFactory;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Applies Ignite-specific configurations to bootstrapped service registries:
 * <ul>
 * <li>Registers Ignite's service initiators</li>
 * <li>Configures Hibernate Search so it works for Ignite</li>
 * </ul>
 * @author Dmitriy Kozlov
 * @author Victor Kadachigov
 *
 */
public class IgniteServiceRegistryInitializer implements ServiceContributor {
	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
//		serviceRegistryBuilder.addInitiator(IgnitePersisterClassResolverInitiator.INSTANCE);
		serviceRegistryBuilder.addService( PersisterClassResolver.class, new IgniteOgmPersisterClassResolver() );
//		serviceRegistryBuilder.addInitiator(CriteriaGridDialectInitiator.INSTANCE);
//		serviceRegistryBuilder.addInitiator(IgniteQueryTranslatorFactoryInitiator.INSTANCE);
		serviceRegistryBuilder.addService( QueryTranslatorFactory.class, new IgniteQueryTranslatorFactory() );
	}

}
