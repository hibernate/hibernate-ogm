/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.Service;
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Bind services requiring a {@link SessionFactory}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class OgmSessionFactoryServiceRegistryImpl extends SessionFactoryServiceRegistryImpl {

	private Configuration configuration = null;

	public OgmSessionFactoryServiceRegistryImpl(ServiceRegistryImplementor parent, SessionFactoryImplementor sessionFactory, Configuration configuration) {
		super( parent, sessionFactory, configuration );
		this.configuration = configuration;
		createServiceBindings();
	}

	public OgmSessionFactoryServiceRegistryImpl(ServiceRegistryImplementor parent, SessionFactoryImplementor sessionFactory, MetadataImplementor metadata) {
		super( parent, sessionFactory, metadata );
		createServiceBindings();
	}

	private void createServiceBindings() {
		for ( SessionFactoryServiceInitiator<?> inititator : OgmSessionFactoryServiceInitiators.LIST ) {
			createServiceBinding( inititator );
		}
	}

	@Override
	public <R extends Service> void configureService(ServiceBinding<R> serviceBinding) {
		if ( Configurable.class.isInstance( serviceBinding.getService() ) ) {
			( (Configurable) serviceBinding.getService() ).configure( configuration.getProperties() );
		}
	}
}
