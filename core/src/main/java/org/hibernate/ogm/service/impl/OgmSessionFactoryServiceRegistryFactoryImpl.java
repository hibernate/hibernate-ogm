/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;

/**
 * Factory for the creation of {@link OgmSessionFactoryServiceRegistryImpl}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 *
 */
public class OgmSessionFactoryServiceRegistryFactoryImpl implements SessionFactoryServiceRegistryFactory {

	private final ServiceRegistryImplementor theBasicServiceRegistry;
	private OgmSessionFactoryServiceRegistryImpl ogmSessionFactoryServiceRegistryImpl;

	public OgmSessionFactoryServiceRegistryFactoryImpl(ServiceRegistryImplementor theBasicServiceRegistry) {
		this.theBasicServiceRegistry = theBasicServiceRegistry;
	}

	@Override
	public SessionFactoryServiceRegistryImpl buildServiceRegistry(SessionFactoryImplementor sessionFactory, Configuration configuration) {
		if ( ogmSessionFactoryServiceRegistryImpl == null ) {
			ogmSessionFactoryServiceRegistryImpl = new OgmSessionFactoryServiceRegistryImpl( theBasicServiceRegistry, sessionFactory, configuration );
		}
		return ogmSessionFactoryServiceRegistryImpl;
	}

	@Override
	public SessionFactoryServiceRegistryImpl buildServiceRegistry(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata) {
		if ( ogmSessionFactoryServiceRegistryImpl == null ) {
			ogmSessionFactoryServiceRegistryImpl = new OgmSessionFactoryServiceRegistryImpl( theBasicServiceRegistry, sessionFactory, metadata );
		}
		return ogmSessionFactoryServiceRegistryImpl;
	}

}
