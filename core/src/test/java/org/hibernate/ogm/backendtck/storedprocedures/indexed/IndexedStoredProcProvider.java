/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.storedprocedures.indexed;

import java.util.Map;

import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * The provider for testing stored procedures on datastore that supports positional parameters
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class IndexedStoredProcProvider extends BaseDatastoreProvider implements Startable, Stoppable, Configurable,
		ServiceRegistryAwareService {
	private ServiceRegistryImplementor serviceRegistry;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return IndexedStoredProcDialect.class;
	}

	@Override
	public void configure(Map configurationValues) {

	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {

	}
}
