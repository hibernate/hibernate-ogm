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
package org.hibernate.ogm.utils.jpa;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.ClassTransformer;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.sql.DataSource;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class GetterPersistenceUnitInfo implements PersistenceUnitInfo {
	private String persistenceUnitName;
	private String persistenceProviderClassName;
	private PersistenceUnitTransactionType transactionType;
	private DataSource jtaDataSource;
	private DataSource nonJtaDataSource;
	private List<String> mappingFileNames = new ArrayList<String>( );
	private List<URL> jarFileUrls = new ArrayList<URL>(  );
	private URL persistenceUnitRootUrl;
	private List<String> managedClassNames = new ArrayList<String>(  );
	private boolean excludeUnlistedClasses;
	private SharedCacheMode sharedCacheMode;
	private ValidationMode validationMode;
	private Properties properties;
	private String persistenceXMLSchemaVersion;
	private ClassLoader classLoader;

	@Override
	public void addTransformer(ClassTransformer transformer) {
		//nothing to do
	}

	@Override
	public ClassLoader getNewTempClassLoader() {
		return null;
	}

	public String getPersistenceUnitName() {
		return persistenceUnitName;
	}

	public void setPersistenceUnitName(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	public String getPersistenceProviderClassName() {
		return persistenceProviderClassName;
	}

	public void setPersistenceProviderClassName(String persistenceProviderClassName) {
		this.persistenceProviderClassName = persistenceProviderClassName;
	}

	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
	}

	public DataSource getJtaDataSource() {
		return jtaDataSource;
	}

	public void setJtaDataSource(DataSource jtaDataSource) {
		this.jtaDataSource = jtaDataSource;
	}

	public DataSource getNonJtaDataSource() {
		return nonJtaDataSource;
	}

	public void setNonJtaDataSource(DataSource nonJtaDataSource) {
		this.nonJtaDataSource = nonJtaDataSource;
	}

	public List<String> getMappingFileNames() {
		return mappingFileNames;
	}

	public void setMappingFileNames(List<String> mappingFileNames) {
		this.mappingFileNames = mappingFileNames;
	}

	public List<URL> getJarFileUrls() {
		return jarFileUrls;
	}

	public void setJarFileUrls(List<URL> jarFileUrls) {
		this.jarFileUrls = jarFileUrls;
	}

	public URL getPersistenceUnitRootUrl() {
		return persistenceUnitRootUrl;
	}

	public void setPersistenceUnitRootUrl(URL persistenceUnitRootUrl) {
		this.persistenceUnitRootUrl = persistenceUnitRootUrl;
	}

	public List<String> getManagedClassNames() {
		return managedClassNames;
	}

	@Override
	public boolean excludeUnlistedClasses() {
		return excludeUnlistedClasses;
	}

	public void setExcludeUnlistedClasses(boolean excludeUnlistedClasses) {
		this.excludeUnlistedClasses = excludeUnlistedClasses;
	}

	public void setManagedClassNames(List<String> managedClassNames) {
		this.managedClassNames = managedClassNames;
	}

	public ValidationMode getValidationMode() {
		return validationMode;
	}

	public void setValidationMode(ValidationMode validationMode) {
		this.validationMode = validationMode;
	}

	public String getPersistenceXMLSchemaVersion() {
		return persistenceXMLSchemaVersion;
	}

	public void setPersistenceXMLSchemaVersion(String persistenceXMLSchemaVersion) {
		this.persistenceXMLSchemaVersion = persistenceXMLSchemaVersion;
	}

	public SharedCacheMode getSharedCacheMode() {
		return sharedCacheMode;
	}

	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
		this.sharedCacheMode = sharedCacheMode;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "GetterPersistenceUnitInfo" );
		sb.append( "{persistenceUnitName='" ).append( persistenceUnitName ).append( '\'' );
		sb.append( ", persistenceProviderClassName='" ).append( persistenceProviderClassName ).append( '\'' );
		sb.append( ", transactionType=" ).append( transactionType );
		sb.append( ", jtaDataSource=" ).append( jtaDataSource );
		sb.append( ", nonJtaDataSource=" ).append( nonJtaDataSource );
		sb.append( ", mappingFileNames=" ).append( mappingFileNames );
		sb.append( ", jarFileUrls=" ).append( jarFileUrls );
		sb.append( ", persistenceUnitRootUrl=" ).append( persistenceUnitRootUrl );
		sb.append( ", managedClassNames=" ).append( managedClassNames );
		sb.append( ", excludeUnlistedClasses=" ).append( excludeUnlistedClasses );
		sb.append( ", sharedCacheMode=" ).append( sharedCacheMode );
		sb.append( ", validationMode=" ).append( validationMode );
		sb.append( ", properties=" ).append( properties );
		sb.append( ", persistenceXMLSchemaVersion='" ).append( persistenceXMLSchemaVersion ).append( '\'' );
		sb.append( ", classLoader=" ).append( classLoader );
		sb.append( '}' );
		return sb.toString();
	}
}
