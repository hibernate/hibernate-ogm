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
import org.hibernate.service.internal.SessionFactoryServiceRegistryImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;

/**
 * Factory for the creation of {@link OgmSessionFactoryServiceRegistryImpl}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
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
