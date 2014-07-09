/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.impl;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Initialize the {@link OptionsService} so that other components can access it using the {@link org.hibernate.service.ServiceRegistry}.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public final class OptionsServiceInitiator implements SessionFactoryServiceInitiator<OptionsService> {

	public static final String MAPPING = "hibernate.ogm.mapping";

	public static final OptionsServiceInitiator INSTANCE = new OptionsServiceInitiator();

	@Override
	public Class<OptionsService> getServiceInitiated() {
		return OptionsService.class;
	}

	@Override
	public OptionsService initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		return new OptionsServiceImpl();
	}

	@Override
	public OptionsService initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		return null;
	}
}
