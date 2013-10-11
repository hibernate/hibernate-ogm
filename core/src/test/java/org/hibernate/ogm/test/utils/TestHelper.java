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

import static org.fest.assertions.Assertions.assertThat;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

import com.arjuna.ats.arjuna.coordinator.TxControl;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public class TestHelper {

	private static final Log log = LoggerFactory.make();
	private static final TestableGridDialect helper = createStoreSpecificHelper();

	static {
		// set 5 hours timeout on transactions: enough for debug, but not too high in case of CI problems.
		TxControl.setDefaultTimeout( 60 * 60 * 2 );
	}

	private TestHelper() {
	}

	public static boolean assertNumberOfEntities(int numberOfEntities, EntityManager em) {
		return assertNumberOfEntities( numberOfEntities, em.unwrap( Session.class ) );
	}

	private static TestableGridDialect createStoreSpecificHelper() {
		for ( GridDialectType gridType : GridDialectType.values() ) {
			Class<?> classForName = gridType.loadTestableGridDialectClass();
			if ( classForName != null ) {
				try {
					TestableGridDialect attempt = (TestableGridDialect) classForName.newInstance();
					log.debugf( "Using TestGridDialect %s", classForName );
					return attempt;
				}
				catch ( Exception e ) {
					// but other errors are not expected:
					log.errorf( e, "Could not load TestGridDialect by name from %s", gridType );
				}
			}
		}
		return new org.hibernate.ogm.test.utils.HashMapTestHelper();
	}

	public static GridDialectType getCurrentDialectType() {
		return GridDialectType.valueFromHelperClass( helper.getClass() );
	}

	public static boolean assertNumberOfEntities(int numberOfEntities, Session session) {
		return assertNumberOfEntities( numberOfEntities, session.getSessionFactory() );
	}

	public static boolean assertNumberOfEntities(int numberOfEntities, SessionFactory sessionFactory) {
		return helper.assertNumberOfEntities( numberOfEntities, sessionFactory );
	}

	public static Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		return helper.extractEntityTuple( sessionFactory, key );
	}

	public static boolean assertNumberOfAssociations(int numberOfAssociations, SessionFactory sessionFactory) {
		boolean result = helper.assertNumberOfAssociations( numberOfAssociations, sessionFactory );
		return result;
	}

	public static boolean backendSupportsTransactions() {
		return helper.backendSupportsTransactions();
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Session session, Class<T> clazz, Serializable id) {
		return (T) session.get( clazz, id );
	}

	public static void dropSchemaAndDatabase(Session session) {
		if ( session != null ) {
			dropSchemaAndDatabase( session.getSessionFactory() );
		}
	}

	public static void dropSchemaAndDatabase(EntityManagerFactory emf) {
		dropSchemaAndDatabase( ( (HibernateEntityManagerFactory) emf ).getSessionFactory() );
	}

	public static void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		// if the factory is closed, we don't have access to the service registry
		if ( sessionFactory != null && !sessionFactory.isClosed() ) {
			try {
				helper.dropSchemaAndDatabase( sessionFactory );
			}
			catch ( Exception e ) {
				log.warn( "Exception while dropping schema and database in test", e );
			}
		}
	}

	public static Map<String, String> getEnvironmentProperties() {
		Map<String, String> environmentProperties = helper.getEnvironmentProperties();
		return environmentProperties == null ? new HashMap<String, String>( 0 ) : environmentProperties;
	}

	public static void checkCleanCache(SessionFactory sessionFactory) {
		assertThat( assertNumberOfEntities( 0, sessionFactory ) ).as( "Entity cache should be empty" ).isTrue();
		assertThat( assertNumberOfAssociations( 0, sessionFactory ) ).as( "Association cache should be empty" ).isTrue();
	}

	/**
	 * Provides a default {@link OgmConfiguration} for tests, using the given set of annotated entity types.
	 *
	 * @param entityTypes the entity types for which to build a configuration
	 * @return a default configuration based on the given types
	 */
	public static OgmConfiguration getDefaultTestConfiguration(Class<?>... entityTypes) {
		OgmConfiguration configuration = new OgmConfiguration();

		// by default use the new id generator scheme...
		configuration.setProperty( Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );

		for ( Map.Entry<String, String> entry : TestHelper.getEnvironmentProperties().entrySet() ) {
			configuration.setProperty( entry.getKey(), entry.getValue() );
		}

		configuration.setProperty( Environment.HBM2DDL_AUTO, "none" );

		// volatile indexes for Hibernate Search (if used)
		configuration.setProperty( "hibernate.search.default.directory_provider", "ram" );
		// disable warnings about unspecified Lucene version
		configuration.setProperty( "hibernate.search.lucene_version", "LUCENE_35" );

		for ( Class<?> aClass : entityTypes ) {
			configuration.addAnnotatedClass( aClass );
		}

		return configuration;
	}
}
