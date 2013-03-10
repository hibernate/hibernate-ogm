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
package org.hibernate.ogm.test.utils.jpa;

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
import javax.transaction.TransactionManager;

import org.hibernate.cfg.Environment;
import org.hibernate.ejb.HibernateEntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;
import org.hibernate.ogm.massindex.OgmMassIndexerFactory;
import org.hibernate.ogm.test.utils.BaseOGMTest;
import org.hibernate.ogm.test.utils.TestHelper;
import org.hibernate.search.hcore.impl.MassIndexerFactoryIntegrator;
import org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.junit.After;
import org.junit.Before;

import static org.hibernate.ogm.test.utils.TestHelper.dropSchemaAndDatabase;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public abstract class JpaTestCase extends BaseOGMTest {

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
		GetterPersistenceUnitInfo info = new GetterPersistenceUnitInfo();
		info.setClassLoader( Thread.currentThread().getContextClassLoader() );
		//we explicitly list them to avoid scanning
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
		final URL persistenceUnitRootUrl = new File("").toURL();
		info.setPersistenceUnitRootUrl( persistenceUnitRootUrl );
		info.setPersistenceXMLSchemaVersion( "2.0" );
		info.setProperties( new Properties() );
		info.setSharedCacheMode( SharedCacheMode.ENABLE_SELECTIVE );
		info.setTransactionType( PersistenceUnitTransactionType.JTA );
		info.setValidationMode( ValidationMode.AUTO );
		info.getProperties().setProperty( Environment.JTA_PLATFORM,
				JBossStandAloneJtaPlatform.class.getName()
		);
		info.getProperties().setProperty( MassIndexerFactoryIntegrator.MASS_INDEXER_FACTORY_CLASSNAME, OgmMassIndexerFactory.class.getName() );
		for ( Map.Entry<String,String> entry : TestHelper.getEnvironmentProperties().entrySet() ) {
			info.getProperties().setProperty( entry.getKey(), entry.getValue() );
		}
		refineInfo( info );
		factory = new HibernateOgmPersistence().createContainerEntityManagerFactory(
				info,
				Collections.EMPTY_MAP
		);
		transactionManager = extractJBossTransactionManager(factory);
	}

	//can be overridden by subclasses
	protected void refineInfo(GetterPersistenceUnitInfo info) {

	}

	/**
	 * Get JBoss TM out of Hibernate
	 */
	public static TransactionManager extractJBossTransactionManager(EntityManagerFactory factory) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) ( (HibernateEntityManagerFactory) factory )
				.getSessionFactory();
		return sessionFactory.getServiceRegistry().getService( JtaPlatform.class ).retrieveTransactionManager();
	}

	/**
	 * We need to make sure failing tests cleanup their association with the transaction manager
	 * so that they don't affect subsequent tests.
	 * @param operationSuccessfull when false, use rollback instead
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
