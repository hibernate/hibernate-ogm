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
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;

/**
 *
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public class TestHelper {

	private static final String[] knownTestDialects = new String[] {
		//Add more TestGridDialect(s) here as needed
		"org.hibernate.ogm.test.utils.EhcacheTestHelper",
		"org.hibernate.ogm.test.utils.InfinispanTestHelper",
		"org.hibernate.ogm.test.utils.MongoDBTestHelper",
		"org.hibernate.ogm.test.utils.HashMapTestHelper" // This should always be the last element or it will be loaded
	};

	private static final Log log = LoggerFactory.make();
	private static final TestableGridDialect helper = createStoreSpecificHelper();

	public static int entityCacheSize(EntityManager em) {
		return entityCacheSize( em.unwrap( Session.class ) );
	}

	private static TestableGridDialect createStoreSpecificHelper() {
		for ( String className : knownTestDialects ) {
			Class<?> classForName = null;
			try {
				classForName = Class.forName( className );
			}
			catch (ClassNotFoundException e) {
				//ignore this: we're searching for the only valid option
			}
			if ( classForName != null ) {
				try {
					TestableGridDialect attempt = (TestableGridDialect) classForName.newInstance();
					log.debugf( "Using TestGridDialect %s", classForName );
					return attempt;
				}
				catch ( Exception e ) {
					//but other errors are not expected:
					log.errorf( e, "Could not load TestGridDialect by name %s", className );
				}
			}
		}
		log.fatal( "Could not load any TestGridDialect implementation!" );
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
	
	public static void dropSchemaAndDatabase(Session session) {
		if ( session != null ) {
			dropSchemaAndDatabase( session.getSessionFactory() );
		}
	}

	public static void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		if ( sessionFactory != null ) {
			helper.dropSchemaAndDatabase( sessionFactory );
		}
	}
}
