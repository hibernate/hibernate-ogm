/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.jpa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.exception.impl.Exceptions;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;
import org.hibernate.ogm.utils.SkippableTestRunner;
import org.hibernate.ogm.utils.TestEntities;
import org.hibernate.ogm.utils.TestEntityManagerFactory;
import org.hibernate.ogm.utils.TestEntityManagerFactory.Scope;
import org.hibernate.ogm.utils.TestEntityManagerFactoryConfiguration;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

/**
 * A JUnit 4 runner for OGM tests using JPA. Based on a given set of entities, it manages a entity manager factory, which is used
 * throughout all test methods of the given test class.
 * <p>
 * The entities of the test are to be returned by a parameterless method annotated with {@link TestEntities} in form of
 * a {@code Class<?>[]}.
 * <p>
 * The used entity manager factory can be obtained by annotating a field of type {@link EntityManagerFactory} with the
 * {@link TestEntityManagerFactory} annotation. The runner will inject the factory in this field then. Depending on the
 * {@link TestEntityManagerFactory#scope() } setting, either the same entity manager factory instance will be used for all test
 * methods of a given test class or a new entity manager factory will be created and injected for each individual test method.
 * <p>
 * Finally the {@link Configuration} used for bootstrapping the factory can optionally be modified by annotating a
 * configuration method with the {@link TestEntityManagerFactoryConfiguration} a shown in the example below.
 * <p>
 * Usage example:
 *
 * <pre>
 * {@code
 * @RunWith(OgmJpaTestRunner.class)
 * public class AnimalFarmTest {
 *
 *     @TestEntityManagerFactory
 *     public EntityManagerFactory entityManagerFactory;
 *
 *     @Test
 *     public void shouldCountAnimals() throws Exception {
 *         EntityManager em = entityManagerFactory.createEntityManager();
 *         ...
 *         em.close();
 *     }
 *
 *    @TestEntityManagerFactoryConfiguration
 *    public static void configure(GetterPersistenceUnitInfo info) {
 *        info.getProperties().setProperty( Environment.MONGODB_ASSOCIATIONS_STORE, AssociationStorage.COLLECTION.name() );
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
 * @see OgmJpaTestCase Base class for tests which is configured with this runner for ease of use
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public class OgmJpaTestRunner extends SkippableTestRunner {

	private final Set<Field> testScopedFactoryFields;
	private final Set<Field> testMethodScopedFactoryFields;

	private EntityManagerFactory testScopedEntityManagerFactory;
	private EntityManagerFactory testMethodScopedEntityManagerFactory;

	public OgmJpaTestRunner(Class<?> klass) throws InitializationError {
		super( klass );

		testScopedFactoryFields = getTestFactoryFields( getTestClass(), Scope.TEST_CLASS );
		testMethodScopedFactoryFields = getTestFactoryFields( getTestClass(), Scope.TEST_METHOD );
	}

	private static Set<Field> getTestFactoryFields(TestClass testClass, TestEntityManagerFactory.Scope scope) {
		Set<Field> testFactoryFields = new HashSet<Field>();

		for ( FrameworkField frameworkField : testClass.getAnnotatedFields( TestEntityManagerFactory.class ) ) {
			Field field = frameworkField.getField();
			if ( scope == field.getAnnotation( TestEntityManagerFactory.class ).scope() ) {
				field.setAccessible( true );
				testFactoryFields.add( field );
			}
		}

		return testFactoryFields;
	}

	@Override
	public void run(RunNotifier notifier) {
		if ( isTestScopedEntityManagerFactoryRequired() ) {
			testScopedEntityManagerFactory = buildEntityManagerFactory();
			injectEntityManagerFactory( null, testScopedFactoryFields, testScopedEntityManagerFactory );
			TestHelper.prepareDatabase( testScopedEntityManagerFactory );
		}

		try {
			super.run( notifier );
		}
		finally {
			if ( testScopedEntityManagerFactory != null ) {
				cleanUpPendingTransactionIfRequired( testScopedEntityManagerFactory );
				TestHelper.dropSchemaAndDatabase( testScopedEntityManagerFactory );
				testScopedEntityManagerFactory.close();
			}
		}
	}

	@Override
	protected void runChild(FrameworkMethod method, RunNotifier notifier) {
		// create test method scoped SF if required; it will be injected in createTest()
		if ( isTestMethodScopedEntityManagerFactoryRequired( method ) ) {
			testMethodScopedEntityManagerFactory = buildEntityManagerFactory();
		}

		try {
			super.runChild( method, notifier );
		}
		finally {
			if ( testMethodScopedEntityManagerFactory != null ) {
				cleanUpPendingTransactionIfRequired( testMethodScopedEntityManagerFactory );
				testMethodScopedEntityManagerFactory.close();
			}
		}
	}

	private boolean isTestScopedEntityManagerFactoryRequired() {
		return !isTestClassSkipped() && !areAllTestMethodsSkipped();
	}

	private boolean isTestMethodScopedEntityManagerFactoryRequired(FrameworkMethod method) {
		return !testMethodScopedFactoryFields.isEmpty() && !super.isTestMethodSkipped( method );
	}

	private void cleanUpPendingTransactionIfRequired(EntityManagerFactory entityManagerFactory) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) entityManagerFactory;
		TransactionManager transactionManager = sessionFactory.getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager();

		try {
			if ( transactionManager != null && transactionManager.getStatus() == Status.STATUS_ACTIVE ) {
				transactionManager.rollback();
			}
		}
		catch (Exception e) {
			throw new IllegalStateException( "Error while cleaning up the pending transactions", e );
		}
	}

	protected EntityManagerFactory buildEntityManagerFactory() {
		try {
			GetterPersistenceUnitInfo info = new GetterPersistenceUnitInfo();
			info.setClassLoader( Thread.currentThread().getContextClassLoader() );
			// we explicitly list them to avoid scanning
			info.setExcludeUnlistedClasses( true );
			info.setJtaDataSource( new NoopDatasource() );
			List<String> classNames = new ArrayList<String>();
			for ( Class<?> clazz : getConfiguredEntityTypes() ) {
				classNames.add( clazz.getName() );
			}
			info.setManagedClassNames( classNames );
			info.setNonJtaDataSource( null );
			info.setPersistenceProviderClassName( HibernateOgmPersistence.class.getName() );
			info.setPersistenceUnitName( "default" );
			final URL persistenceUnitRootUrl = new File( "" ).toURI().toURL();
			info.setPersistenceUnitRootUrl( persistenceUnitRootUrl );
			info.setPersistenceXMLSchemaVersion( "2.0" );
			info.setProperties( new Properties() );
			info.setSharedCacheMode( SharedCacheMode.ENABLE_SELECTIVE );
			info.setTransactionType( PersistenceUnitTransactionType.RESOURCE_LOCAL );
			info.setValidationMode( ValidationMode.AUTO );

			for ( Map.Entry<String, String> entry : TestHelper.getDefaultTestSettings().entrySet() ) {
				info.getProperties().setProperty( entry.getKey(), entry.getValue() );
			}
			applyTestSpecificSettings( info );

			return new HibernateOgmPersistence().createContainerEntityManagerFactory( info, Collections.EMPTY_MAP );
		}
		catch (Exception e) {
			throw new IllegalStateException( "Unable to build the entity manager factory", e );
		}
	}

	private Class<?>[] getConfiguredEntityTypes() {
		for ( FrameworkMethod frameworkMethod : getTestClass().getAnnotatedMethods( TestEntities.class ) ) {
			Class<?>[] entityTypes = invokeTestEntitiesMethod( frameworkMethod );

			if ( entityTypes == null || entityTypes.length == 0 ) {
				throw new IllegalArgumentException( "Define at least a single annotated entity" );
			}

			return entityTypes;
		}

		throw new IllegalStateException( "The entities of the test must be retrievable via a parameterless method which is annotated with "
				+ TestEntities.class.getSimpleName() + " and returns Class<?>[]." );
	}

	private Class<?>[] invokeTestEntitiesMethod(FrameworkMethod frameworkMethod) {
		Method method = frameworkMethod.getMethod();
		method.setAccessible( true );

		if ( method.getReturnType() != Class[].class || method.getParameterTypes().length > 0 ) {
			throw new IllegalStateException( "Method annotated with " + TestEntities.class.getSimpleName()
					+ " must have no parameters and must return Class<?>[]." );
		}

		Class<?>[] entityTypes = null;

		try {
			entityTypes = (Class<?>[]) method.invoke( super.createTest() );
		}
		catch (Exception e) {
			Exceptions.<RuntimeException>sneakyThrow( e );
		}

		return entityTypes;
	}

	private void applyTestSpecificSettings(GetterPersistenceUnitInfo info) {
		try {
			for ( FrameworkMethod frameworkMethod : getTestClass().getAnnotatedMethods( TestEntityManagerFactoryConfiguration.class ) ) {
				Method method = frameworkMethod.getMethod();
				method.setAccessible( true );
				method.invoke( super.createTest(), info );
			}
		}
		catch (Exception e) {
			Exceptions.<RuntimeException>sneakyThrow( e );
		}
	}

	@Override
	protected Object createTest() throws Exception {
		Object test = super.createTest();

		// inject SFs as per given scopes
		if ( !testScopedFactoryFields.isEmpty() ) {
			injectEntityManagerFactory( test, testScopedFactoryFields, testScopedEntityManagerFactory );
		}
		if ( !testMethodScopedFactoryFields.isEmpty() ) {
			injectEntityManagerFactory( test, testMethodScopedFactoryFields, testMethodScopedEntityManagerFactory );
		}

		return test;
	}

	private void injectEntityManagerFactory(Object test, Iterable<Field> fields, EntityManagerFactory sessionFactory) {
		for ( Field field : fields ) {
			try {
				if ( ( test == null && Modifier.isStatic( field.getModifiers() ) ) ||
						( test != null && !Modifier.isStatic( field.getModifiers() ) ) ) {
					field.set( test, sessionFactory );
				}
			}
			catch (Exception e) {
				throw new RuntimeException( "Can't inject entity manager factory into field " + field );
			}
		}
	}
}
