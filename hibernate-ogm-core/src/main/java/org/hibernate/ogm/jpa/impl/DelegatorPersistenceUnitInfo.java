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
package org.hibernate.ogm.jpa.impl;

import java.net.URL;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

import org.hibernate.ejb.HibernatePersistence;

/**
 * Delegate most PersistenceUnitInfo method except for:
 *  - getPersistenceProviderClassName which is set to Hibernate EntityManager's persistence provider
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DelegatorPersistenceUnitInfo implements PersistenceUnitInfo {
	private PersistenceUnitInfo delegator;

	public DelegatorPersistenceUnitInfo(PersistenceUnitInfo info) {
		this.delegator = info;
	}

	@Override
	public String getPersistenceUnitName() {
		return delegator.getPersistenceUnitName();
	}

	@Override
	public String getPersistenceProviderClassName() {
		return HibernatePersistence.class.getName();
	}

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return delegator.getTransactionType();
	}

	@Override
	public DataSource getJtaDataSource() {
		return delegator.getJtaDataSource();
	}

	@Override
	public DataSource getNonJtaDataSource() {
		return delegator.getNonJtaDataSource();
	}

	@Override
	public List<String> getMappingFileNames() {
		return delegator.getMappingFileNames();
	}

	@Override
	public List<URL> getJarFileUrls() {
		return delegator.getJarFileUrls();
	}

	@Override
	public URL getPersistenceUnitRootUrl() {
		return delegator.getPersistenceUnitRootUrl();
	}

	@Override
	public List<String> getManagedClassNames() {
		return delegator.getManagedClassNames();
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return delegator.excludeUnlistedClasses();
	}

	@Override
	public SharedCacheMode getSharedCacheMode() {
		return delegator.getSharedCacheMode();
	}

	@Override
	public ValidationMode getValidationMode() {
		return delegator.getValidationMode();
	}

	@Override
	public Properties getProperties() {
		return delegator.getProperties();
	}

	@Override
	public String getPersistenceXMLSchemaVersion() {
		return delegator.getPersistenceXMLSchemaVersion();
	}

	@Override
	public ClassLoader getClassLoader() {
		return delegator.getClassLoader();
	}

	@Override
	public void addTransformer(ClassTransformer transformer) {
		delegator.addTransformer( transformer );
	}

	@Override
	public ClassLoader getNewTempClassLoader() {
		return delegator.getNewTempClassLoader();
	}
}
