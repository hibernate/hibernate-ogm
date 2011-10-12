/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.metadata;

import java.util.Map;

import org.infinispan.manager.CacheContainer;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.impl.Version;
import org.hibernate.ogm.datastore.infinispan.impl.CacheManagerServiceProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.infinispan.InfinispanDialect;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.service.Service;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * Start and stop the Infinispan CacheManager with the SearchFactory
 * TODO abstract that to other grids
 *
 * @author Emmanuel Bernard
 */
public class GridMetadataManager implements Service, ServiceRegistryAwareService, Stoppable, Startable {
	private CacheManagerServiceProvider manager;
	private final TypeTranslator typeTranslator;
	private GridDialect gridDialect;
	private Map<?, ?> configurationValues;
	private ServiceRegistryImplementor serviceRegistry;

	public GridMetadataManager(Map<?,?> configurationValues) {
		Version.touch();
		typeTranslator = new TypeTranslator();
		gridDialect = new InfinispanDialect();
		manager = new CacheManagerServiceProvider();
		this.configurationValues = configurationValues;
	}

	//TODO abstract to other grids
	public CacheContainer getCacheContainer() { return manager.getService(); }

	//TODO move to a *Implementor interface
	public TypeTranslator getTypeTranslator() { return typeTranslator; }

	public GridDialect getGridDialect() { return gridDialect; }

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void stop() {
		manager.stop();
	}

	@Override
	public void start() {
		this.manager.start( serviceRegistry, configurationValues );
		// no longer needed, get rid of it
		this.configurationValues = null;
		this.serviceRegistry = null;
	}
}
