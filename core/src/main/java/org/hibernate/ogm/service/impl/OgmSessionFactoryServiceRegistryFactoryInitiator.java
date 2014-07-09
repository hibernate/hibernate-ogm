/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class OgmSessionFactoryServiceRegistryFactoryInitiator extends OptionalServiceInitiator<SessionFactoryServiceRegistryFactory> {

	public static final OgmSessionFactoryServiceRegistryFactoryInitiator INSTANCE = new OgmSessionFactoryServiceRegistryFactoryInitiator();

	@Override
	public Class<SessionFactoryServiceRegistryFactory> getServiceInitiated() {
		return SessionFactoryServiceRegistryFactory.class;
	}

	@Override
	protected SessionFactoryServiceRegistryFactory buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		return new OgmSessionFactoryServiceRegistryFactoryImpl( registry );
	}

	@Override
	protected StandardServiceInitiator<SessionFactoryServiceRegistryFactory> backupInitiator() {
		return SessionFactoryServiceRegistryFactoryInitiator.INSTANCE;
	}
}
