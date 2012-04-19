/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.StartStoppable;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Execute SessionFactoryObserver events on a compliant DatastoreProvider
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DatastoreProviderToSessionFactoryObserverAdaptor implements SessionFactoryObserver {
	private SessionFactoryServiceRegistry serviceRegistry;
	private DatastoreProvider service;
	private Configuration configuration;

	public DatastoreProviderToSessionFactoryObserverAdaptor(Configuration configuration, SessionFactoryServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
		this.configuration = configuration;
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		service = serviceRegistry.getService( DatastoreProvider.class );
		if ( service instanceof StartStoppable ) {
			StartStoppable.class.cast( service ).start( configuration, (SessionFactoryImplementor) factory );
		}
		//clear heavy objects
		this.configuration = null;
		this.serviceRegistry = null;
	}

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		if ( service instanceof  SessionFactoryObserver ) {
			StartStoppable.class.cast( service ).stop();
		}
	}
}
