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
package org.hibernate.ogm.test.jpa.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.TransactionManager;

import org.junit.After;
import org.junit.Before;

import org.hibernate.cfg.Environment;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;
import org.hibernate.ogm.test.utils.PackagingRule;
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
		transactionManager = getJBossTransactionManager();

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
		final URL persistenceUnitRootUrl = PackagingRule.getTargetDir().toURI().toURL();
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

	/**
	 * Initializes a JBossTS Standalone TransactionManager to not use permanent journals.
	 * See jbossts-properties.xml for configuration.
	 */
	private static TransactionManager getJBossTransactionManager() {
		TransactionManager transactionManager = new JBossTSStandaloneTransactionManagerLookup().getTransactionManager( null );
		return transactionManager;
	}

	@After
	public void closeFactory() {
		factory.close();
		factory = null;
	}

}
