package org.hibernate.ogm.test.jpa.util;

import java.io.File;
import java.net.MalformedURLException;
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
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.internal.arjuna.objectstore.VolatileStore;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import org.hibernate.cfg.Environment;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;
import org.hibernate.transaction.JBossTSStandaloneTransactionManagerLookup;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
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
	public void createFactory() throws MalformedURLException {
		//set in memory tx persistence store
		arjPropertyManager.getCoordinatorEnvironmentBean().setActionStore( VolatileStore.class.getName() );
		transactionManager = new JBossTSStandaloneTransactionManagerLookup().getTransactionManager( null );

		GetterPersistenceUnitInfo info = new GetterPersistenceUnitInfo();
		info.setClassLoader( Thread.currentThread().getContextClassLoader() );
		//we explicitly list them to avoid scanning
		info.setExcludeUnlistedClasses( true );
		info.setJtaDataSource( null );
		List<String> classNames = new ArrayList<String>();
		for ( Class<?> clazz : getEntities() ) {
			classNames.add( clazz.getName() );
		}
		info.setManagedClassNames( classNames );
		info.setNonJtaDataSource( null );
		info.setPersistenceProviderClassName( HibernateOgmPersistence.class.getName() );
		info.setPersistenceUnitName( "default" );
		final URL persistenceUnitRootUrl = new File( "/Users/manu/projects/notbackedup/git/ogm" ).toURI().toURL();
		info.setPersistenceUnitRootUrl( persistenceUnitRootUrl );
		info.setPersistenceXMLSchemaVersion( "2.0" );
		info.setProperties( new Properties() );
		info.setSharedCacheMode( SharedCacheMode.ENABLE_SELECTIVE );
		info.setTransactionType( PersistenceUnitTransactionType.JTA );
		info.setValidationMode( ValidationMode.AUTO );
		info.getProperties().setProperty( Environment.TRANSACTION_MANAGER_STRATEGY,
				JBossTSStandaloneTransactionManagerLookup.class.getName()
		);
		factory = new HibernateOgmPersistence().createContainerEntityManagerFactory(
				info,
				Collections.EMPTY_MAP
		);
	}

	@After
	public void closeFactory() {
		factory.close();
		factory = null;
	}

}
