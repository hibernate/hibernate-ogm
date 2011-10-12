/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.utils;

import java.io.Serializable;

import org.infinispan.Cache;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.metadata.GridMetadataManager;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;
import org.hibernate.service.UnknownServiceException;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class TestHelper {
	public static Cache getEntityCache(Session session) {
		final GridMetadataManager gridManager = getGridMetadataManager(session.getSessionFactory());
		return getEntityCache(gridManager);
	}

	public static Cache getEntityCache(SessionFactory sessionFactory) {
		final GridMetadataManager gridManager = getGridMetadataManager(sessionFactory);
		return getEntityCache(gridManager);
	}

	private static Cache getEntityCache(GridMetadataManager gridManager) {
		return gridManager.getCacheContainer().getCache( GridMetadataManagerHelper.ENTITY_CACHE );
	}

	public static Cache getAssociationCache(Session session) {
		final GridMetadataManager manager = getGridMetadataManager(session.getSessionFactory());
		return manager.getCacheContainer().getCache( GridMetadataManagerHelper.ASSOCIATION_CACHE );
	}

	public static Cache getAssociationCache(SessionFactory sessionFactory) {
		final GridMetadataManager gridManager = getGridMetadataManager(sessionFactory);
		return getAssociationCache( gridManager );
	}

	private static Cache getAssociationCache(GridMetadataManager gridManager) {
		return gridManager.getCacheContainer().getCache( GridMetadataManagerHelper.ASSOCIATION_CACHE );
	}

	private static GridMetadataManager getGridMetadataManager(SessionFactory factory) {
        ServiceRegistryImplementor serviceRegistry = ((SessionFactoryImplementor) factory).getServiceRegistry();
        try {
            return serviceRegistry.getService(GridMetadataManager.class);
        }
        catch (UnknownServiceException e) {
        	throw new RuntimeException( "Wrong OGM configuration: observer not set", e );
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Session session, Class<T> clazz, Serializable id) {
		return (T) session.get(clazz, id);
	}
}
