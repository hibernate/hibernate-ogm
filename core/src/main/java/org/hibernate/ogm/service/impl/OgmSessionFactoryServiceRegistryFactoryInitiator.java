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

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.service.internal.SessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistryFactory;

/**
 * @author Davide D'Alto <davide@hibernate.org>
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
