/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.metadata;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.PropertyKey;

/**
 * Helper class to be refactored and abstracted from specific grid implementation
 * @author Emmanuel Bernard
 */
public class GridMetadataManagerHelper {
	public static final String ENTITY_CACHE = "ENTITIES";
	//TODO same or different? Customizable?
	public static final String PROPERTY_CACHE = ENTITY_CACHE;

	public static GridMetadataManager getGridMetadataManager(SessionFactoryImplementor factory) {
		final SessionFactoryObserver sessionFactoryObserver = factory.getFactoryObserver();
		if ( sessionFactoryObserver instanceof GridMetadataManager ) {
			return ( GridMetadataManager ) sessionFactoryObserver;
		}
		else {
			StringBuilder error = new StringBuilder("Cannot get CacheManager for OGM. ");
			if ( sessionFactoryObserver == null ) {
				error.append("SessionFactoryObserver not configured");
			}
			else {
				error.append("SessionFactoryObserver not of type " + GridMetadataManager.class);
			}
			throw new HibernateException( error.toString() );
		}
	}

	public static Cache<EntityKey, Map<String, Object>> getEntityCache(SessionFactoryImplementor factory) {
		return getEntityCache( getGridMetadataManager(factory) );
	}

	public static Cache<EntityKey, Map<String, Object>> getEntityCache(GridMetadataManager manager) {
		final CacheContainer cacheContainer = manager.getCacheContainer();
		final Cache<EntityKey, Map<String, Object>> cache = cacheContainer.getCache( ENTITY_CACHE );
		return cache;
	}

	public static Cache<PropertyKey, List<Map<String,Object>>> getPropertyCache(SessionFactoryImplementor factory) {
		return getPropertyCache( getGridMetadataManager( factory ) );
	}

	public static Cache<PropertyKey, List<Map<String,Object>>> getPropertyCache(GridMetadataManager manager) {
		final CacheContainer cacheContainer = manager.getCacheContainer();
		final Cache<PropertyKey, List<Map<String,Object>>> cache = cacheContainer.getCache( PROPERTY_CACHE );
		return cache;
	}
}
