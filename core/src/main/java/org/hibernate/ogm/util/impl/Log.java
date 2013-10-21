/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2013 Red Hat Inc. and/or its affiliates and other contributors
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

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.annotation.ElementType;

import javax.transaction.SystemException;

import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.hql.internal.ast.QuerySyntaxException;
import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.options.spi.AnnotationConverter;
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

	@Message(id = 8, value = "DatastoreManager class [%1$s] cannot be found in classpath try with: %2$s")
	HibernateException datastoreClassCannotBeFound(String className, String availableShortcuts);

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

	@Message(id = 18, value = "JTA transaction begin failed")
	TransactionException jtaTransactionBeginFailed(@Cause Exception e);

	@Message(id = 19, value = "JTA transaction commit failed")
	TransactionException jtaCommitFailed(@Cause Exception e);

	@Message(id = 20, value = "JTA transaction rollback failed")
	TransactionException jtaRollbackFailed(@Cause Exception e);

	@Message(id = 21, value = "Unable to mark JTA transaction for rollback")
	TransactionException unableToMarkTransactionForRollback(@Cause Exception e);

	@Message(id = 22, value = "Could not determine transaction status")
	TransactionException jtaCouldNotDetermineStatus(@Cause SystemException se);

	@Message(id = 23, value = "Unable to set transaction timeout to '%1$s'")
	TransactionException unableToSetTimeout(@Cause SystemException se, int timeout);

	@Message(id = 24, value = "Syntax error in query: [%1$s]")
	HibernateException querySyntaxException(@Cause QuerySyntaxException qse, String queryString);

	@LogMessage(level = ERROR)
	@Message(id = 25, value = "Batch indexing was interrupted")
	void interruptedBatchIndexing();

	@Message(id = 26, value = "Illegal discriminator type: '%1$s'")
	HibernateException illegalDiscrimantorType(String name);

	@Message(id = 27, value = "Could not convert string to discriminator object")
	HibernateException unableToConvertStringToDiscriminator(@Cause Exception e);

	@LogMessage(level = DEBUG)
	@Message(id = 28, value = "Created query object '%2$s' from HQL/JP-QL query '%1$s'.")
	void createdQuery(String hqlQuery, Object queryObject);

	@Message(id = 29, value = "Given class %2$s is no implementation/sub-type of %1$s.")
	HibernateException givenImplementationClassIsOfWrongType(String expectedClassName, String actualClassName);

	@Message(id = 30, value = "Cannot instantiate query parser service %s.")
	HibernateException unableToInstantiateQueryParserService(String parserServiceClassName, @Cause Exception e);

	@LogMessage(level = WARN)
	@Message(id = 31, value = "OgmMassIndexer doesn't support the configuration option '%s'. Its setting will be ignored.")
	void unsupportedIndexerConfigurationOption(String optionName);

	@Message(id = 32, value = "Unable to support mapping subtypes that are not interfaces: %1$s")
	HibernateException mappingSubtypeNotInterface(Class<?> mappingType);

	@Message(id = 33, value = "Unable to create new proxy instance")
	HibernateException cannotCreateNewProxyInstance(@Cause Exception e);

	@Message(id = 34, value = "Annotation cannot be converted using %1$s")
	HibernateException cannotConvertAnnotation(Class<? extends AnnotationConverter<?>> converterClass, @Cause Exception e);

	@Message(id = 36, value = "Unable to load %1$s method from %2$s ")
	HibernateException unableToLoadContext(String methodName, Class<?> contextClass, @Cause Exception e);

	@Message(id = 37, value = "Unable to create global context proxy for type %1$s")
	HibernateException cannotCreateGlobalContextProxy(Class<?> contextClass, @Cause Exception e);

	@Message(id = 38, value = "Unable to create entity context proxy for type %1$s")
	HibernateException cannotCreateEntityContextProxy(Class<?> contextClass, @Cause Exception e);

	@Message(id = 39, value = "Unable to create property context proxy for type %1$s")
	HibernateException cannotCreatePropertyContextProxy(Class<?> contextClass, @Cause Exception e);

	@Message(id = 40, value = "The current datastore can not be configured via configuration type %1$s.")
	HibernateException getWrongDatastoreConfigurationTypeException(String datastoreConfigurationTypeName);

	@Message(id = 41, value = "The given propery %1$s#%2$s with element type %3$s does not exist.")
	HibernateException getPropertyDoesNotExistException(String typeName, String property, ElementType elementType);

	@Message(id = 42, value = "The given element type %1$s is neither FIELD nor METHOD.")
	HibernateException getUnsupportedElementTypeException(ElementType elementType);
}
