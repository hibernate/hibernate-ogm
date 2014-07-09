/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class ConfigurationServiceInitiator implements StandardServiceInitiator<ConfigurationService> {
	public static ConfigurationServiceInitiator INSTANCE = new ConfigurationServiceInitiator();
	@Override
	public ConfigurationService initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new ConfigurationService( configurationValues );
	}

	@Override
	public Class<ConfigurationService> getServiceInitiated() {
		return ConfigurationService.class;
	}
}
