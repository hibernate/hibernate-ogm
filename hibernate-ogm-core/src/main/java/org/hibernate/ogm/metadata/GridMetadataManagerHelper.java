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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.RowKey;

/**
 * Helper class to be refactored and abstracted from specific grid implementation
 * @author Emmanuel Bernard
 */
public class GridMetadataManagerHelper {
	public static final String ENTITY_CACHE = "ENTITIES";
	public static final String ASSOCIATION_CACHE = "ASSOCIATIONS";
	public static final String IDENTIFIER_CACHE = "IDENTIFIERS";

	public static GridMetadataManager getGridMetadataManager(SessionFactoryImplementor factory) {
		//TODO use service registry instead of observer
		GridMetadataManager service = factory.getServiceRegistry().getService( GridMetadataManager.class );
		if ( service != null ) {
			return service;
		}
		else {
			throw new HibernateException( "Cannot get CacheManager for OGM. OGM Services not configured" );
		}
	}

	public static Cache<EntityKey, Map<String, Object>> getEntityCache(SessionFactoryImplementor factory) {
		return getEntityCache( getGridMetadataManager(factory) );
	}

	public static Cache<AssociationKey, Map<RowKey,Map<String,Object>>> getAssociationCache(SessionFactoryImplementor factory) {
		return getAssociationCache( getGridMetadataManager( factory ) );
	}

	public static Cache<EntityKey, Map<String, Object>> getEntityCache(GridMetadataManager manager) {
		return manager.getEntityCache();
		// TODO : cleanup this unnecessary indirection
	}

	public static Cache<RowKey, Object> getIdentifierCache(GridMetadataManager manager) {
		return manager.getIdentifierCache();
		// TODO : cleanup this unnecessary indirection
	}

	public static Cache<AssociationKey, Map<RowKey,Map<String,Object>>> getAssociationCache(GridMetadataManager manager) {
		return manager.getAssociationCache();
		// TODO : cleanup this unnecessary indirection
	}
}
