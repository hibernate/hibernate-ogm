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
package org.hibernate.ogm.test.simpleentity;

import java.io.InputStream;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.infinispan.impl.CacheManagerServiceProvider;
import org.hibernate.ogm.transaction.infinispan.impl.DummyTransactionManagerLookup;
import org.hibernate.ogm.transaction.infinispan.impl.JTATransactionManagerTransactionFactory;
import org.hibernate.testing.junit.functional.annotations.HibernateTestCase;
import org.hibernate.transaction.JBossTSStandaloneTransactionManagerLookup;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.getAssociationCache;
import static org.hibernate.ogm.test.utils.TestHelper.getEntityCache;

/**
 * A base class for all OGM tests.
 *
 * @author Emmnauel Bernand
 * @author Hardy Ferentschik
 */
public abstract class OgmTestCase extends HibernateTestCase {

	protected static SessionFactory sessions;
	private Session session;

	public OgmTestCase() {
		super();
	}

	public OgmTestCase(String name) {
		super( name );
	}

	public Session openSession() throws HibernateException {
		rebuildSessionFactory();
		session = getSessions().openSession();
		return session;
	}

	public Session openSession(Interceptor interceptor) throws HibernateException {
		rebuildSessionFactory();
		session = getSessions().openSession( interceptor );
		return session;
	}

	private void rebuildSessionFactory() {
		if ( sessions == null ) {
			try {
				buildConfiguration();
			}
			catch ( Exception e ) {
				throw new HibernateException( e );
			}
		}
	}

	protected void setSessions(SessionFactory sessions) {
		OgmTestCase.sessions = sessions;
	}

	protected SessionFactory getSessions() {
		return sessions;
	}

	protected SessionFactoryImplementor sfi() {
		return (SessionFactoryImplementor) getSessions();
	}

	//FIXME clear cache when this happens
	protected void runSchemaGeneration() {

	}

	//FIXME clear cache when this happens
	protected void runSchemaDrop() {
		
	}

	@Override
	protected void buildConfiguration() throws Exception {
		if ( getSessions() != null ) {
			getSessions().close();
		}
		try {
			setCfg( new OgmConfiguration() );

			//Grid specific configuration
			cfg.setProperty( CacheManagerServiceProvider.INFINISPAN_CONFIGURATION_RESOURCENAME, "infinispan-local.xml" );
			cfg.setProperty( "hibernate.transaction.default_factory_class", JTATransactionManagerTransactionFactory.class.getName() );
			cfg.setProperty( Environment.TRANSACTION_MANAGER_STRATEGY, JBossTSStandaloneTransactionManagerLookup.class.getName() );


			//Other configurations
			// by default use the new id generator scheme...
			cfg.setProperty( Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
			configure( cfg );
			if ( recreateSchema() ) {
				cfg.setProperty( Environment.HBM2DDL_AUTO, "none" );
			}
			for ( String aPackage : getAnnotatedPackages() ) {
				getCfg().addPackage( aPackage );
			}
			for ( Class<?> aClass : getAnnotatedClasses() ) {
				getCfg().addAnnotatedClass( aClass );
			}
			for ( String xmlFile : getXmlFiles() ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				getCfg().addInputStream( is );
			}
			setSessions( getCfg().buildSessionFactory( /* new TestInterceptor() */ ) );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	protected void handleUnclosedResources() {
		if ( session != null && session.isOpen() ) {
			if ( session.isConnected() ) {
				if ( session.getTransaction().isActive() ) {
					session.getTransaction().rollback();
				}
			}
			session.close();
			session = null;
			fail( "unclosed session" );
		}
		else {
			session = null;
		}
		if ( sessions != null && !sessions.isClosed() ) {
			sessions.close();
			sessions = null;
		}
	}

	@Override
	protected void closeResources() {
		try {
			if ( session != null && session.isOpen() ) {
				if ( session.isConnected() ) {
					if ( session.getTransaction().isActive() ) {
						session.getTransaction().rollback();
					}
				}
				session.close();
			}
		}
		catch ( Exception ignore ) {
		}
		try {
			if ( sessions != null ) {
				sessions.close();
				sessions = null;
			}
		}
		catch ( Exception ignore ) {
		}
	}

	public void checkCleanCache() {
		assertThat(getEntityCache( sessions )).as("Entity cache should be empty").hasSize( 0 );
		assertThat(getAssociationCache( sessions )).as("Association cache should be empty").hasSize( 0 );
	}
}
