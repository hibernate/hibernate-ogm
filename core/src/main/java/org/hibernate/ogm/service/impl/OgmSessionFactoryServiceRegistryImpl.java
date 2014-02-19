/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.service.impl;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.ogm.datastore.spi.StartStoppable;
import org.hibernate.service.Service;
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;

/**
 * Bind services requiring a {@link org.hibernate.SessionFactory}.
 *
 * Specifically customize the list of SessionFactory services and
 * execute the {@link StartStoppable} start calls.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 *
 */
public class OgmSessionFactoryServiceRegistryImpl extends SessionFactoryServiceRegistryImpl {

	private Configuration configuration = null;
	private SessionFactoryImplementor sessionFactory = null;

	public OgmSessionFactoryServiceRegistryImpl(ServiceRegistryImplementor parent, SessionFactoryImplementor sessionFactory, Configuration configuration) {
		super( parent, sessionFactory, configuration );
		this.configuration = configuration;
		this.sessionFactory = sessionFactory;
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

	@Override
	public <R extends Service> void startService(ServiceBinding<R> serviceBinding) {
		super.startService( serviceBinding );
		if ( StartStoppable.class.isInstance( serviceBinding.getService() ) ) {
			( (StartStoppable) serviceBinding.getService() ).start( configuration, sessionFactory );
		}
	}

}
