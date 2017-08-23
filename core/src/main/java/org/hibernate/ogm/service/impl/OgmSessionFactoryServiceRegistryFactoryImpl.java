/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.internal.SessionFactoryServiceRegistryBuilderImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceContributor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;

/**
 * Factory for the creation of OGM's {@link SessionFactoryServiceRegistry}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 * @author Guillaume Smet
 */
public class OgmSessionFactoryServiceRegistryFactoryImpl implements SessionFactoryServiceRegistryFactory {

	private final ServiceRegistryImplementor theBasicServiceRegistry;

	public OgmSessionFactoryServiceRegistryFactoryImpl(ServiceRegistryImplementor theBasicServiceRegistry) {
		this.theBasicServiceRegistry = theBasicServiceRegistry;
	}

	@Override
	public SessionFactoryServiceRegistry buildServiceRegistry(SessionFactoryImplementor sessionFactory, SessionFactoryOptions options) {
		final ClassLoaderService cls = options.getServiceRegistry().getService( ClassLoaderService.class );
		final SessionFactoryServiceRegistryBuilderImpl builder = new SessionFactoryServiceRegistryBuilderImpl( theBasicServiceRegistry );

		// Add the OGM services to the builder
		for ( SessionFactoryServiceInitiator<?> initiator : OgmSessionFactoryServiceInitiators.LIST ) {
			builder.addInitiator( initiator );
		}

		for ( SessionFactoryServiceContributor contributor : cls.loadJavaServices( SessionFactoryServiceContributor.class ) ) {
			contributor.contribute( builder );
		}

		return builder.buildSessionFactoryServiceRegistry( sessionFactory, options );
	}
}
