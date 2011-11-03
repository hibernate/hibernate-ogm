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

import org.hibernate.ogm.datastore.impl.DatastoreServices;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.infinispan.Cache;

import java.util.Map;

/**
 * Start and stop the Infinispan CacheManager with the SearchFactory
 * TODO abstract that to other grids
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class GridMetadataManager implements Service, ServiceRegistryAwareService, Configurable {
	private TypeTranslator typeTranslator;
	private ServiceRegistryImplementor serviceRegistry;
	private DatastoreServices datastoreServices;
	private InfinispanDatastoreProvider datastoreProvider;

	//public TypeTranslator getTypeTranslator() { return typeTranslator; }

	//public GridDialect getGridDialect() { return datastoreServices.getGridDialect(); }

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public Cache<EntityKey, Map<String, Object>> getEntityCache() {
		return (Cache<EntityKey, Map<String, Object>>) datastoreProvider.getCache( GridMetadataManagerHelper.ENTITY_CACHE );
	}

	public Cache<RowKey, Object> getIdentifierCache() {
		return (Cache<RowKey, Object>) datastoreProvider.getCache( GridMetadataManagerHelper.IDENTIFIER_CACHE );
	}

	public Cache<AssociationKey, Map<RowKey, Map<String, Object>>> getAssociationCache() {
		return (Cache<AssociationKey, Map<RowKey, Map<String, Object>>>) datastoreProvider.getCache( GridMetadataManagerHelper.ASSOCIATION_CACHE );
	}

	@Override
	public void configure(Map configurationValues) {
		typeTranslator = serviceRegistry.getService( TypeTranslator.class );
		datastoreServices = serviceRegistry.getService(DatastoreServices.class);
		datastoreProvider = (InfinispanDatastoreProvider) serviceRegistry.getService(DatastoreProvider.class);
	}
}
