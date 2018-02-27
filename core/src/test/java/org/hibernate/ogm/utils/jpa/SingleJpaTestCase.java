/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.jpa;

import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.Status;
import javax.transaction.TransactionManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;
import org.hibernate.ogm.utils.SkippableTestRunner;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 */
@RunWith(SkippableTestRunner.class)
public abstract class SingleJpaTestCase {

	private EntityManagerFactory factory;
	private TransactionManager transactionManager;

	public EntityManagerFactory getFactory() {
		return factory;
	}

	public abstract Class<?>[] getEntities();

	@Before
	public void createFactory() throws Throwable {
		GetterPersistenceUnitInfo info = new GetterPersistenceUnitInfo();
		info.setClassLoader( Thread.currentThread().getContextClassLoader() );
		// we explicitly list them to avoid scanning
		info.setExcludeUnlistedClasses( true );
		info.setJtaDataSource( new NoopDatasource() );
		List<String> classNames = new ArrayList<String>();
		for ( Class<?> clazz : getEntities() ) {
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
		refineInfo( info );
		factory = new HibernateOgmPersistence().createContainerEntityManagerFactory( info, Collections.EMPTY_MAP );
		transactionManager = extractJBossTransactionManager( factory );
	}

	// can be overridden by subclasses
	protected void refineInfo(GetterPersistenceUnitInfo info) {

	}

	/**
	 * @return Return the transaction manager in the case where one is available. Can be {@code null}.
	 * A transaction manager will be available if JBoss Transaction is on the classpath. Where it is in use depends on
	 * the current persistence unit under test and its persistence type setting.
	 *
	 * @throws Exception
	 */
	public TransactionManager getTransactionManager() throws Exception {
		return transactionManager;
	}

	/**
	 * Get JBoss TM out of Hibernate
	 */
	private static TransactionManager extractJBossTransactionManager(EntityManagerFactory factory) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) factory;
		return sessionFactory.getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager();
	}

	protected ServiceRegistryImplementor getServiceRegistry() {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) getFactory();
		ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
		return serviceRegistry;
	}

	@After
	public void closeFactory() throws Exception {
		if ( transactionManager != null && transactionManager.getStatus() == Status.STATUS_ACTIVE ) {
			transactionManager.rollback();
		}

		if ( factory != null ) {
			if ( factory.isOpen() ) {
				dropSchemaAndDatabase( factory );
				factory.close();
				factory = null;
			}
			else {
				factory = null;
			}
		}
	}

}
