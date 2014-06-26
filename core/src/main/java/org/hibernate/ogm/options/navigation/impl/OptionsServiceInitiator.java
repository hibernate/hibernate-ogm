/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initialize the {@link OptionsService} so that other components can access it using the {@link org.hibernate.service.ServiceRegistry}.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public final class OptionsServiceInitiator implements StandardServiceInitiator<OptionsService> {

	public static final String MAPPING = "hibernate.ogm.mapping";

	public static final OptionsServiceInitiator INSTANCE = new OptionsServiceInitiator();

	@Override
	public Class<OptionsService> getServiceInitiated() {
		return OptionsService.class;
	}

	@Override
	public OptionsService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new OptionsServiceImpl();
	}
}
