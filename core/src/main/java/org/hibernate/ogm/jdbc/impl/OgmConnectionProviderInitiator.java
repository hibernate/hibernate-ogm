/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jdbc.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.ogm.service.impl.OptionalServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmConnectionProviderInitiator extends OptionalServiceInitiator<ConnectionProvider> {
	public static OgmConnectionProviderInitiator INSTANCE = new OgmConnectionProviderInitiator();

	@Override
	public Class<ConnectionProvider> getServiceInitiated() {
		return ConnectionProvider.class;
	}

	@Override
	protected ConnectionProvider buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		return new NoopConnectionProvider();
	}

	@Override
	protected StandardServiceInitiator<ConnectionProvider> backupInitiator() {
		return ConnectionProviderInitiator.INSTANCE;
	}
}
