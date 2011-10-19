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

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

import org.hibernate.ogm.cfg.impl.Version;
import org.hibernate.ogm.datastore.infinispan.impl.CacheManagerServiceProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.infinispan.InfinispanDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * Start and stop the Infinispan CacheManager with the SearchFactory
 * TODO abstract that to other grids
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class GridMetadataManager implements Service, ServiceRegistryAwareService, Stoppable, Startable {
	private final CacheManagerServiceProvider manager;
	private final TypeTranslator typeTranslator;
	private final GridDialect gridDialect;
	private Map<?, ?> configurationValues;
	private ServiceRegistryImplementor serviceRegistry;

	private Cache<AssociationKey, Map<RowKey, Map<String, Object>>> associationCache;
	private Cache<EntityKey, Map<String, Object>> entityCache;
	private Cache<RowKey, Object> identifierCache;

	public GridMetadataManager(Map<?,?> configurationValues) {
		Version.touch();
		this.typeTranslator = new TypeTranslator();
		this.gridDialect = new InfinispanDialect();
		this.manager = new CacheManagerServiceProvider();
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
		this.associationCache = getCacheContainer().getCache( GridMetadataManagerHelper.ASSOCIATION_CACHE );
		this.entityCache = getCacheContainer().getCache( GridMetadataManagerHelper.ENTITY_CACHE );
		this.identifierCache = getCacheContainer().getCache( GridMetadataManagerHelper.IDENTIFIER_CACHE );
	}

	public Cache<EntityKey, Map<String, Object>> getEntityCache() {
		return entityCache;
	}

	public Cache<RowKey, Object> getIdentifierCache() {
		return identifierCache;
	}

	public Cache<AssociationKey, Map<RowKey, Map<String, Object>>> getAssociationCache() {
		return associationCache;
	}
}
