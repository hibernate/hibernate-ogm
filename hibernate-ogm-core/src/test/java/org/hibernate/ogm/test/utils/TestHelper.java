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
import java.lang.annotation.Target;

import org.infinispan.Cache;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.ogm.metadata.GridMetadataManager;
import org.hibernate.ogm.metadata.GridMetadataManagerHelper;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class TestHelper {
	public static Cache getEntityCache(Session session) {
		final SessionFactoryObserver observer = getObserver( session.getSessionFactory() );
		return getEntityCache(observer);
	}

	public static Cache getEntityCache(SessionFactory sessionFactory) {
		final SessionFactoryObserver observer = getObserver( sessionFactory );
		return getEntityCache(observer);
	}

	private static Cache getEntityCache(SessionFactoryObserver observer) {
		return ( ( GridMetadataManager ) observer ).getCacheContainer().getCache( GridMetadataManagerHelper.ENTITY_CACHE );
	}

	public static Cache getAssociationCache(Session session) {
		final SessionFactoryObserver observer = getObserver( session.getSessionFactory() );
		return ( ( GridMetadataManager ) observer ).getCacheContainer().getCache( GridMetadataManagerHelper.ASSOCIATION_CACHE );
	}

	public static Cache getAssociationCache(SessionFactory sessionFactory) {
		final SessionFactoryObserver observer = getObserver( sessionFactory );
		return getAssociationCache( observer );
	}

	private static Cache getAssociationCache(SessionFactoryObserver observer) {
		return ( ( GridMetadataManager ) observer ).getCacheContainer().getCache( GridMetadataManagerHelper.ASSOCIATION_CACHE );
	}

	private static SessionFactoryObserver getObserver(SessionFactory factory) {
		final SessionFactoryObserver observer = ( ( SessionFactoryImplementor ) factory ).getFactoryObserver();
		if (observer == null) {
			throw new RuntimeException( "Wrong OGM configuration: observer not set" );
		}
		return observer;
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Session session, Class<T> clazz, Serializable id) {
		return (T) session.get(clazz, id);
	}
}
