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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.grid.EntityKey;
import org.infinispan.Cache;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;

import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ASSOCIATION_STORE;
import static org.hibernate.ogm.datastore.spi.DefaultDatastoreNames.ENTITY_STORE;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public class TestHelper {

	public static int entityCacheSize(EntityManager em) {
		return entityCacheSize( em.unwrap( Session.class ) );
	}

	public static int entityCacheSize(Session session) {
		return entityCacheSize( session.getSessionFactory() );
	}

	public static int entityCacheSize(SessionFactory sessionFactory) {
		return getEntityCache( sessionFactory ).size();
	}

	private static Cache getEntityCache(Session session) {
		return getEntityCache( session.getSessionFactory() );
	}

	public static Map extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return (Map) getEntityCache( sessionFactory ).get( key );
	}

	private static Cache getEntityCache(SessionFactory sessionFactory) {
		InfinispanDatastoreProvider castProvider = getProvider(sessionFactory);
		return castProvider.getCache(ENTITY_STORE);
	}

	private static InfinispanDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ((SessionFactoryImplementor) sessionFactory).getServiceRegistry().getService(DatastoreProvider.class);
		if ( ! (InfinispanDatastoreProvider.class.isInstance(provider) ) ) {
			throw new RuntimeException("Not testing with Infinispan, cannot extract underlying cache");
		}
		return InfinispanDatastoreProvider.class.cast(provider);
	}

	public static Cache getAssociationCache(SessionFactory sessionFactory) {
		InfinispanDatastoreProvider castProvider = getProvider(sessionFactory);
		return castProvider.getCache(ASSOCIATION_STORE);
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Session session, Class<T> clazz, Serializable id) {
		return (T) session.get(clazz, id);
	}
}
