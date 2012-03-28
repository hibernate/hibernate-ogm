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
package org.hibernate.ogm.util.impl;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends BasicLogger {

	@LogMessage(level = INFO)
	@Message(id = 1, value = "Hibernate OGM %1$s")
	void version(String versionString);

	@LogMessage(level = WARN)
	@Message(id = 2, value = "Could not find any META-INF/persistence.xml file in the classpath. " +
						"Unable to build Persistence Unit %1$s")
	void persistenceXmlNotFoundInClassPath(String unitName);

	@LogMessage(level = INFO)
	@Message(id = 3, value = "Use default transaction factory (use a TransactionManager exclusively to pilot the transaction)")
	void usingDefaultTransactionFactory();

	@Message(id = 4, value = "Unable to find or initialize Infinispan CacheManager")
	HibernateException unableToInitializeInfinispan(@Cause RuntimeException e);

	@Message(id = 5, value = "%1$s is not a subclass of DatastoreManager. Update " + DatastoreProviderInitiator.DATASTORE_PROVIDER)
	HibernateException notADatastoreManager(String propertyValue);

	@Message(id = 6, value = "Cannot instantiate DatastoreManager %1$s")
	HibernateException unableToInstantiateDatastoreManager(String managerClassName, @Cause Exception e);

	@Message(id = 7, value = "DatastoreManager property is of unknown type %1$s")
	HibernateException unknownDatastoreManagerType(String managerType);

	@Message(id = 8, value = "DatastoreManager class [%1$s] cannot be found in classpath")
	HibernateException datastoreClassCannotBeFound(String className);

	@Message(id = 9, value = DatastoreProviderInitiator.DATASTORE_PROVIDER + " has not been defined and no DatastoreManager could be guessed")
	HibernateException noDatastoreConfigured();

	@Message(id = 10, value = "GridDialect class [%1$s] cannot be found in classpath")
	HibernateException dialectClassCannotBeFound(String className);

	@Message(id = 11, value = "Cannot instantiate GridDialect class [%1$s]")
	HibernateException cannotInstantiateGridDialect(Class<?> dialectClass, @Cause Exception e);

	@Message(id = 12, value = "GridDialect property is of unknown type %1$s")
	HibernateException gridDialectPropertyOfUnknownType(Class<?> type);

	@Message(id = 13, value = "%1$s property does not implement GridDialect interface")
	HibernateException doesNotImplementGridDialect(String value);

	@Message(id = 14, value = "%1$s has no constructor accepting DatasourceProvider")
	HibernateException gridDialectHasNoProperConstrutor(Class<?> dialectClass);

	@Message(id = 15, value = "Expected DatastoreProvider %2$s but found %1$s")
	HibernateException unexpectedDatastoreProvider(Class<?> found, Class<?> expected);

	@LogMessage(level = INFO)
	@Message(id = 16, value = "NoSQL Datastore provider: %1$s")
	void useDatastoreProvider(String datastoreProviderClass);

	@LogMessage(level = INFO)
	@Message(id = 17, value = "Grid Dialect: %1$s")
	void useGridDialect(String gridDialectClass);

	@Message(id = 18, value = "Cannot load %1$s driver")
	HibernateException unableToLoadDriver(String driverName);
}
