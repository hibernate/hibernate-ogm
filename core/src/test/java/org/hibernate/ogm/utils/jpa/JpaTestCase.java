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
import javax.transaction.TransactionManager;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;
import org.hibernate.ogm.massindex.impl.OgmMassIndexerFactory;
import org.hibernate.ogm.utils.GridDialectSkippableTestRunner;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.search.hcore.impl.MassIndexerFactoryProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt;
 */
@RunWith(GridDialectSkippableTestRunner.class)
public abstract class JpaTestCase {

	private EntityManagerFactory factory;
	private TransactionManager transactionManager;

	public EntityManagerFactory getFactory() {
		return factory;
	}

	public abstract Class<?>[] getEntities();

	public TransactionManager getTransactionManager() throws Exception {
		return transactionManager;
	}

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
		final URL persistenceUnitRootUrl = new File( "" ).toURL();
		info.setPersistenceUnitRootUrl( persistenceUnitRootUrl );
		info.setPersistenceXMLSchemaVersion( "2.0" );
		info.setProperties( new Properties() );
		info.setSharedCacheMode( SharedCacheMode.ENABLE_SELECTIVE );
		info.setTransactionType( PersistenceUnitTransactionType.JTA );
		info.setValidationMode( ValidationMode.AUTO );
		info.getProperties().setProperty( MassIndexerFactoryProvider.MASS_INDEXER_FACTORY_CLASSNAME, OgmMassIndexerFactory.class.getName() );
		for ( Map.Entry<String, String> entry : TestHelper.getEnvironmentProperties().entrySet() ) {
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
	 * Get JBoss TM out of Hibernate
	 */
	public static TransactionManager extractJBossTransactionManager(EntityManagerFactory factory) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) ( (HibernateEntityManagerFactory) factory ).getSessionFactory();
		return sessionFactory.getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager();
	}

	/**
	 * We need to make sure failing tests cleanup their association with the transaction manager
	 * so that they don't affect subsequent tests.
	 *
	 * @param operationSuccessfull
	 *            when false, use rollback instead
	 * @throws Exception
	 */
	protected final void commitOrRollback(boolean operationSuccessfull) throws Exception {
		if ( operationSuccessfull ) {
			getTransactionManager().commit();
		}
		else {
			getTransactionManager().rollback();
		}
	}

	@After
	public void closeFactory() {
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
