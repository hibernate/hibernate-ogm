/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.ogm.service.impl.OptionalServiceInitiator;
import org.hibernate.persister.internal.PersisterClassResolverInitiator;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmPersisterClassResolverInitiator extends OptionalServiceInitiator<PersisterClassResolver> {
	public static final OgmPersisterClassResolverInitiator INSTANCE = new OgmPersisterClassResolverInitiator();

	@Override
	public Class<PersisterClassResolver> getServiceInitiated() {
		return PersisterClassResolver.class;
	}

	@Override
	protected PersisterClassResolver buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		return new OgmPersisterClassResolver();
	}

	@Override
	protected StandardServiceInitiator<PersisterClassResolver> backupInitiator() {
		return PersisterClassResolverInitiator.INSTANCE;
	}
}
