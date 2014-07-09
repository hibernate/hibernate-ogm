/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public abstract class OptionalServiceInitiator<S extends Service> implements StandardServiceInitiator<S> {

	@Override
	public S initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		if ( registry.getService( ConfigurationService.class ).isOgmOn() ) {
			return buildServiceInstance( configurationValues, registry );
		}
		else {
			StandardServiceInitiator<S> initiator = backupInitiator();
			return initiator != null ? initiator.initiateService( configurationValues, registry ) : null;
		}
	}

	protected abstract S buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry);

	protected abstract StandardServiceInitiator<S> backupInitiator();
}
