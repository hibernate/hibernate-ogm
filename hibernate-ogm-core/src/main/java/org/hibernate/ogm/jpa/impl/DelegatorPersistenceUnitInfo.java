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
