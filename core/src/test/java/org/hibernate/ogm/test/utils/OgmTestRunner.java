/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.test.utils.TestSessionFactory.Scope;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

/**
 * A JUnit 4 runner for OGM tests. Based on a given set of entities, it manages a session factory, which is used
 * throughout all test methods of the given test class.
 * <p>
 * The entities of the test are to be returned by a parameterless method annotated with {@link TestEntities} in form of
 * a {@code Class<?>[]}.
 * <p>
 * The used session factory can be obtained by annotating a field of type {@link SessionFactory} with the
 * {@link TestSessionFactory} annotation. The runner will inject the factory in this field then. Depending on the
 * {@link TestSessionFactory#scope() } setting, either the same session factory instance will be used for all test
 * methods of a given test class or a new session factory will be created and injected for each individual test method.
 * <p>
 * Finally the {@link Configuration} used for bootstrapping the factory can optionally be modified by annotating a
 * configuration method with the {@link SessionFactoryConfiguration} a shown in the example below.
 * <p>
 * Usage example:
 *
 * <pre>
 * {@code
 * @RunWith(OgmTestRunner.class)
 * public class AnimalFarmTest {
 *
 *     @TestSessionFactory
 *     public SessionFactory sessions;
 *
 *     @Test
 *     public void shouldCountAnimals() throws Exception {
 *         Session session = sessions.openSession();
 *         ...
 *         session.close();
 *     }
 *
 *    @SessionFactoryConfiguration
 *    public static void configure(Configuration cfg) {
 *        cfg.setProperty(
 *            Environment.MONGODB_ASSOCIATIONS_STORE,
 *            AssociationStorage.COLLECTION.name()
 *        );
 *    }
 *
 *    @TestEntities
 *    public Class<?>[] getTestEntities() {
 *        return new Class<?>[]{ PolarBear.class, Giraffe.class };
 *    }
 * }
 * }
 * </pre>
 *
 * @see OgmTestCase Base class for tests which is configured with this runner for ease of use
 * @author Gunnar Morling
 */
public class OgmTestRunner extends GridDialectSkippableTestRunner {

	private static final Log LOG = LoggerFactory.make();

	private final Set<Field> testScopedFactoryFields;
	private final Set<Field> testMethodScopedFactoryFields;

	private SessionFactory testScopedSessionFactory;
	private SessionFactory testMethodScopedSessionFactory;

	public OgmTestRunner(Class<?> klass) throws InitializationError {
		super( klass );

		testScopedFactoryFields = getTestFactoryFields( getTestClass(), Scope.TEST_CLASS );
		testMethodScopedFactoryFields = getTestFactoryFields( getTestClass(), Scope.TEST_METHOD );
	}

	private static Set<Field> getTestFactoryFields(TestClass testClass, TestSessionFactory.Scope scope) {
		Set<Field> testFactoryFields = new HashSet<Field>();

		for ( FrameworkField frameworkField : testClass.getAnnotatedFields( TestSessionFactory.class ) ) {
			Field field = frameworkField.getField();
			if ( scope == field.getAnnotation( TestSessionFactory.class ).scope() ) {
				field.setAccessible( true );
				testFactoryFields.add( field );
			}
		}

		return testFactoryFields;
	}

	@Override
	public void run(RunNotifier notifier) {
		testScopedSessionFactory = buildSessionFactory();

		//inject shared SF into static fields, if any
		injectSessionFactory( null, testScopedFactoryFields, testScopedSessionFactory );

		try {
			super.run( notifier );
		}
		finally {
			cleanUpPendingTransactionIfRequired();
			TestHelper.dropSchemaAndDatabase( testScopedSessionFactory );
			testScopedSessionFactory.close();
		}
	}

	@Override
	protected void runChild(FrameworkMethod method, RunNotifier notifier) {
		// create test method scoped SF if required; it will be injected in createTest()
		if ( !testMethodScopedFactoryFields.isEmpty() ) {
			testMethodScopedSessionFactory = buildSessionFactory();
		}

		try {
			super.runChild( method, notifier );
		}
		finally {
			if ( testMethodScopedSessionFactory != null ) {
				testMethodScopedSessionFactory.close();
			}
		}
	}

	private void cleanUpPendingTransactionIfRequired() {
		TransactionManager transactionManager = ( (SessionFactoryImplementor) testScopedSessionFactory )
				.getServiceRegistry()
				.getService( JtaPlatform.class )
				.retrieveTransactionManager();

		try {
			if ( transactionManager != null && transactionManager.getTransaction() != null ) {
				LOG.warn( "The test started a transaction but failed to commit it or roll it back. Going to roll it back." );
				transactionManager.rollback();
			}
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	protected SessionFactory buildSessionFactory() {
		Class<?>[] classes = getConfiguredEntityTypes();
		Configuration configuration = createConfiguration( classes );

		return configuration.buildSessionFactory();
	}

	private Class<?>[] getConfiguredEntityTypes() {
		for ( FrameworkMethod frameworkMethod : getTestClass().getAnnotatedMethods( TestEntities.class ) ) {
			Method method = frameworkMethod.getMethod();
			method.setAccessible( true );

			if ( method.getReturnType() != Class[].class || method.getParameterTypes().length > 0 ) {
				throw new IllegalStateException( "Method annotated with " + TestEntities.class.getSimpleName()
						+ " must have no parameters and must return Class<?>[]." );
			}
			try {
				return (Class<?>[]) method.invoke( super.createTest() );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}

		throw new IllegalStateException( "The entities of the test must be retrievable via a parameterless method which is annotated with "
				+ TestEntities.class.getSimpleName() + " and returns Class<?>[]." );
	}

	private Configuration createConfiguration(Class<?>[] entityTypes) {
		if ( entityTypes == null || entityTypes.length == 0 ) {
			throw new IllegalArgumentException( "Define at least a single annotated entity" );
		}

		OgmConfiguration configuration = TestHelper.getDefaultTestConfiguration( entityTypes );
		invokeTestConfigurationMethod( configuration );

		return configuration;
	}

	private void invokeTestConfigurationMethod(OgmConfiguration configuration) {
		try {
			for ( FrameworkMethod frameworkMethod : getTestClass().getAnnotatedMethods( SessionFactoryConfiguration.class ) ) {
				Method method = frameworkMethod.getMethod();
				method.setAccessible( true );
				method.invoke( super.createTest(), configuration );
			}
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	protected Object createTest() throws Exception {
		Object test = super.createTest();

		// inject SFs as per given scopes
		if ( !testScopedFactoryFields.isEmpty() ) {
			injectSessionFactory( test, testScopedFactoryFields, testScopedSessionFactory );
		}
		if ( !testMethodScopedFactoryFields.isEmpty() ) {
			injectSessionFactory( test, testMethodScopedFactoryFields, testMethodScopedSessionFactory );
		}

		return test;
	}

	private void injectSessionFactory(Object test, Iterable<Field> fields, SessionFactory sessionFactory) {
		for ( Field field : fields ) {
			try {
				if ( ( test == null && Modifier.isStatic( field.getModifiers() ) ) ||
						( test != null && !Modifier.isStatic( field.getModifiers() ) ) ) {
					field.set( test, sessionFactory );
				}
			}
			catch (Exception e) {
				throw new RuntimeException( "Can't inject session factory into field " + field );
			}
		}
	}
}
