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
import org.hibernate.ogm.grid.EntityKey;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;

/**
 *
 * Run tests with -Dogm-testhelper-implementation=org.hibernate.ogm.test.utils.HashMapTestHelper
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public class TestHelper {

	private static final TestGridDialect helper = createStoreSpecificHelper();

	public static int entityCacheSize(EntityManager em) {
		return entityCacheSize( em.unwrap( Session.class ) );
	}

	private static TestGridDialect createStoreSpecificHelper() {
		String helperClassName = System.getProperty( "ogm-testhelper-implementation", "org.hibernate.ogm.test.utils.HashMapTestHelper" );
		try {
			return (TestGridDialect) Class.forName( helperClassName ).newInstance();
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
		return null;
	}

	public static int entityCacheSize(Session session) {
		return entityCacheSize( session.getSessionFactory() );
	}

	public static int entityCacheSize(SessionFactory sessionFactory) {
		return helper.entityCacheSize( sessionFactory );
	}

	public static Map extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return helper.extractEntityTuple( sessionFactory, key );
	}

	public static int associationCacheSize(SessionFactory sessionFactory) {
		return helper.associationCacheSize( sessionFactory );
	}

	public static boolean backendSupportsTransactions() {
		return helper.backendSupportsTransactions();
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Session session, Class<T> clazz, Serializable id) {
		return (T) session.get(clazz, id);
	}
}
