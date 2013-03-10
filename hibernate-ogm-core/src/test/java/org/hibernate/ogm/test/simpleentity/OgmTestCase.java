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

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.assertNumberOfAssociations;
import static org.hibernate.ogm.test.utils.TestHelper.assertNumberOfEntities;
import static org.hibernate.ogm.test.utils.TestHelper.dropSchemaAndDatabase;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.test.utils.GridDialectType;
import org.hibernate.ogm.test.utils.SkipByGridDialect;
import org.hibernate.ogm.test.utils.TestHelper;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.testing.FailureExpected;

/**
 * A base class for all OGM tests.
 *
 * This class is a mix of SearchTestCase from HSearch 4 and OgmTestCase from the Core 3.6 days
 * It could get some love to clean this mess
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public abstract class OgmTestCase extends TestCase {

	static {
		TestHelper.initializeHelpers();
	}

	private static final Log log = LoggerFactory.make();
	protected SessionFactory sessions;
	private Session session;

	protected static Configuration cfg;
	private static Class<?> lastTestClass;

	@Before
	public void setUp() throws Exception {
		if ( cfg == null || lastTestClass != getClass() ) {
			buildConfiguration();
			lastTestClass = getClass();
		}
	}

	protected String[] getXmlFiles() {
		return new String[] { };
	}

	protected static void setCfg(Configuration cfg) {
		OgmTestCase.cfg = cfg;
	}

	protected static Configuration getCfg() {
		return cfg;
	}

	protected void configure(Configuration cfg) {
	}

	@After
	public void tearDown() throws Exception {
		handleUnclosedResources();
		closeResources();
	}

	protected abstract Class<?>[] getAnnotatedClasses();

	protected boolean recreateSchema() {
		return true;
	}

	protected String[] getAnnotatedPackages() {
		return new String[] { };
	}

	protected SearchFactoryImplementor getSearchFactoryImpl() {
		FullTextSession s = Search.getFullTextSession( openSession() );
		s.close();
		SearchFactory searchFactory = s.getSearchFactory();
		return (SearchFactoryImplementor) searchFactory;
	}

	private void reportSkip(Skip skip) {
		reportSkip( skip.reason, skip.testDescription );
	}

	protected void reportSkip(String reason, String testDescription) {
		StringBuilder builder = new StringBuilder();
		builder.append( "*** skipping test [" );
		builder.append( fullTestName() );
		builder.append( "] - " );
		builder.append( testDescription );
		builder.append( " : " );
		builder.append( reason );
		log.warn( builder.toString() );
	}

	protected Skip buildSkip(GridDialectType dialect, String comment) {
		StringBuilder buffer = new StringBuilder();
		buffer.append( "skipping database-specific test [" );
		buffer.append( fullTestName() );
		buffer.append( "] for dialect [" );
		buffer.append( dialect.name() );
		buffer.append( ']' );

		if ( StringHelper.isNotEmpty(comment) ) {
			buffer.append( "; " ).append( comment );
		}

		return new Skip( buffer.toString(), null );
	}

	protected <T extends Annotation> T locateAnnotation(Class<T> annotationClass, Method runMethod) {
		T annotation = runMethod.getAnnotation( annotationClass );
		if ( annotation == null ) {
			annotation = getClass().getAnnotation( annotationClass );
		}
		if ( annotation == null ) {
			annotation = runMethod.getDeclaringClass().getAnnotation( annotationClass );
		}
		return annotation;
	}

	protected final Skip determineSkipByGridDialect(Method runMethod) throws Exception {
		SkipByGridDialect skipForDialectAnn = locateAnnotation( SkipByGridDialect.class, runMethod );
		if ( skipForDialectAnn != null ) {
			for ( GridDialectType gridDialectType : skipForDialectAnn.value() ) {
				if ( gridDialectType.equals( TestHelper.getCurrentDialectType() ) ) {
					return buildSkip( gridDialectType, skipForDialectAnn.comment() );
				}
			}
		}
		return null;
	}

	protected static class Skip {
		private final String reason;
		private final String testDescription;

		public Skip(String reason, String testDescription) {
			this.reason = reason;
			this.testDescription = testDescription;
		}
	}

	@Override
	protected void runTest() throws Throwable {
		Method runMethod = findTestMethod();
		FailureExpected failureExpected = locateAnnotation( FailureExpected.class, runMethod );
		try {
			super.runTest();
			if ( failureExpected != null ) {
				throw new FailureExpectedTestPassedException();
			}
		}
		catch ( FailureExpectedTestPassedException t ) {
			closeResources();
			throw t;
		}
		catch ( Throwable t ) {
			if ( t instanceof InvocationTargetException) {
				t = ( ( InvocationTargetException ) t ).getTargetException();
			}
			if ( t instanceof IllegalAccessException ) {
				t.fillInStackTrace();
			}
			closeResources();
			if ( failureExpected != null ) {
				StringBuilder builder = new StringBuilder();
				if ( StringHelper.isNotEmpty( failureExpected.message() ) ) {
					builder.append( failureExpected.message() );
				}
				else {
					builder.append( "ignoring @FailureExpected test" );
				}
				builder.append( " (" )
						.append( failureExpected.jiraKey() )
						.append( ")" );
				log.warn( builder.toString(), t );
			}
			else {
				throw t;
			}
		}
	}

	@Override
	public void runBare() throws Throwable {
		Method runMethod = findTestMethod();

		final Skip skip = determineSkipByGridDialect( runMethod );
		if ( skip != null ) {
			reportSkip( skip );
			return;
		}

		setUp();
		try {
			runTest();
		}
		finally {
			tearDown();
		}
	}

		public String fullTestName() {
		return this.getClass().getName() + "#" + this.getName();
	}

	private Method findTestMethod() {
		String fName = getName();
		assertNotNull( fName );
		Method runMethod = null;
		try {
			runMethod = getClass().getMethod( fName );
		}
		catch ( NoSuchMethodException e ) {
			fail( "Method \"" + fName + "\" not found" );
		}
		if ( !Modifier.isPublic(runMethod.getModifiers()) ) {
			fail( "Method \"" + fName + "\" should be public" );
		}
		return runMethod;
	}

	private static class FailureExpectedTestPassedException extends Exception {
		public FailureExpectedTestPassedException() {
			super( "Test marked as @FailureExpected, but did not fail!" );
		}
	}




	public Session openSession() throws HibernateException {
		rebuildSessionFactory();
		session = getSessions().openSession();
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
		this.sessions = sessions;
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

	protected void runSchemaDrop() {
		dropSchemaAndDatabase( session );
	}

	protected void buildConfiguration() throws Exception {
		closeSessionFactory();
		try {
			setCfg( new OgmConfiguration() );

			//Other configurations
			// by default use the new id generator scheme...
			cfg.setProperty( Configuration.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );

			for( Map.Entry<String,String> entry : TestHelper.getEnvironmentProperties().entrySet() ) {
				cfg.setProperty( entry.getKey(), entry.getValue() );
			}

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
		closeSessionFactory();
	}

	private void closeSessionFactory() {
		if ( sessions != null ) {
			if ( !sessions.isClosed() ) {
				dropSchemaAndDatabase( sessions );
				sessions.close();
				sessions = null;
			}
			else {
				sessions = null;
			}
		}
	}

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
			closeSessionFactory();
		}
		catch ( Exception ignore ) {
		}
	}

	public void checkCleanCache() {
		assertThat( assertNumberOfEntities( 0, sessions ) ).as("Entity cache should be empty").isTrue();
		assertThat( assertNumberOfAssociations( 0, sessions ) ).as("Association cache should be empty").isTrue();
	}
}
