/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.Serializable;
import java.lang.annotation.ElementType;

import javax.transaction.SystemException;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.TransactionException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.exception.EntityAlreadyExistsException;
import org.hibernate.ogm.options.spi.AnnotationConverter;
import org.hibernate.service.spi.ServiceException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages and exceptions used by Hibernate OGM core. Dialects may provide extensions of this interface with their
 * own specific log messages.
 * <p>
 * The following intervals for the message ids are to be used:
 * <ul>
 * <li>1-1000: core</li>
 * <li>1101-1200: infinispan</li>
 * <li>1201-1300: mongob</li>
 * <li>1301-1400: couchdb</li>
 * <li>1401-1500: neo4j</li>
 * <li>1501-1600: ehcache</li>
 * </ul>
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 * @author Gunnar Morling
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

	@Message(id = 11, value = "Cannot instantiate GridDialect class [%1$s]")
	HibernateException cannotInstantiateGridDialect(@FormatWith(ClassObjectFormatter.class) Class<?> dialectClass, @Cause Exception e);

	@Message(id = 14, value = "%1$s has no constructor accepting DatasourceProvider")
	HibernateException gridDialectHasNoProperConstructor(@FormatWith(ClassObjectFormatter.class) Class<?> dialectClass);

	@Message(id = 15, value = "Expected DatastoreProvider %2$s but found %1$s")
	HibernateException unexpectedDatastoreProvider(@FormatWith(ClassObjectFormatter.class) Class<?> found, @FormatWith(ClassObjectFormatter.class) Class<?> expected);

	@LogMessage(level = INFO)
	@Message(id = 16, value = "NoSQL Datastore provider: %1$s")
	void useDatastoreProvider(@FormatWith(ClassObjectFormatter.class) Class<?> datastoreProviderClass);

	@LogMessage(level = INFO)
	@Message(id = 17, value = "Grid Dialect: %1$s")
	void useGridDialect(@FormatWith(ClassObjectFormatter.class) Class<?> gridDialectClass);

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
	HibernateException querySyntaxException(@Cause Exception qse, String queryString);

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

	@LogMessage(level = WARN)
	@Message(id = 31, value = "OgmMassIndexer doesn't support the configuration option '%s'. Its setting will be ignored.")
	void unsupportedIndexerConfigurationOption(String optionName);

	@Message(id = 32, value = "Unable to support mapping subtypes that are not interfaces: %1$s")
	HibernateException mappingSubtypeNotInterface(@FormatWith(ClassObjectFormatter.class) Class<?> mappingType);

	@Message(id = 33, value = "Unable to create new proxy instance")
	HibernateException cannotCreateNewProxyInstance(@Cause Exception e);

	@Message(id = 34, value = "Annotation cannot be converted using %1$s")
	HibernateException cannotConvertAnnotation(@FormatWith(ClassObjectFormatter.class) Class<? extends AnnotationConverter<?>> converterClass, @Cause Exception e);

	@Message(id = 36, value = "Unable to load %1$s method from %2$s ")
	HibernateException unableToLoadContext(String methodName, @FormatWith(ClassObjectFormatter.class) Class<?> contextClass, @Cause Exception e);

	@Message(id = 37, value = "Unable to create global context proxy for type %1$s")
	HibernateException cannotCreateGlobalContextProxy(@FormatWith(ClassObjectFormatter.class) Class<?> contextClass, @Cause Exception e);

	@Message(id = 38, value = "Unable to create entity context proxy for type %1$s")
	HibernateException cannotCreateEntityContextProxy(@FormatWith(ClassObjectFormatter.class) Class<?> contextClass, @Cause Exception e);

	@Message(id = 39, value = "Unable to create property context proxy for type %1$s")
	HibernateException cannotCreatePropertyContextProxy(@FormatWith(ClassObjectFormatter.class) Class<?> contextClass, @Cause Exception e);

	@Message(id = 41, value = "The given propery %1$s#%2$s with element type %3$s does not exist.")
	HibernateException getPropertyDoesNotExistException(String typeName, String property, ElementType elementType);

	@Message(id = 42, value = "The given element type %1$s is neither FIELD nor METHOD.")
	HibernateException getUnsupportedElementTypeException(ElementType elementType);

	@Message(id = 43, value = "Cannot instantiate type %1$s. Does it define a default constructor?")
	HibernateException unableToInstantiateType(@FormatWith(ClassObjectFormatter.class) Class<?> clazz, @Cause Exception e);

	@Message(id = 44, value = "Cannot load class %2$s specified via configuration property '%1$s'")
	HibernateException unableToLoadClass(String propertyName, String className, @Cause Exception e);

	@Message(id = 45, value = "Type %2$s specified via configuration property '%1$s' is not a sub-type of expected type %3$s")
	HibernateException unexpectedClassType(String propertyName, @FormatWith(ClassObjectFormatter.class) Class<?> clazz, @FormatWith(ClassObjectFormatter.class) Class<?> expectedClass);

	@Message(id = 46, value = "Object %2$s of type %3$s specified via configuration property '%1$s' is not of the expected type %4$s")
	HibernateException unexpectedInstanceType(String propertyName, String instance, @FormatWith(ClassObjectFormatter.class) Class<?> actualClass, @FormatWith(ClassObjectFormatter.class) Class<?> expectedClass);

	@Message(id = 47, value = "Either an option configurator may be specified via configuration property '%1$s' or OgmConfiguration#configureOptions() may be called, but not both at the same time.")
	HibernateException ambigiousOptionConfiguration(String propertyName);

	@Message(id = 48, value = "Unknown association storage strategy: [%s]. Supported values are: %s" )
	HibernateException unknownAssociationStorageStrategy(String databaseName, String supportedValues);

	@Message(id = 49, value = "The value set for the configuration property '" + OgmProperties.PORT + "' must be a number between 1 and 65535. Found '%s'.")
	HibernateException illegalPortValue(int value);

	@Message(id = 50, value = "The value set for the configuration property '%1$s' must be an integer number. Found '%2$s'.")
	HibernateException notAnInteger(String propertyName, String value);

	@Message(id = 51, value = "Unknown value given for configuration property '%1$s'; Found '%2$s', but supported values are: %3$s" )
	HibernateException unknownEnumerationValue(String propertyName, String value, String supportedValues);

	@Message(id = 52, value = "Missing value for property '%s'")
	HibernateException missingConfigurationProperty(String propertyName);

	@Message(id = 53, value = "Value of unsupported type given for configuration property '%1$s': '%2$s'")
	HibernateException unsupportedPropertyType(String propertyName, String value);

	@Message(id = 54, value = "It is not possible to add or poll operations from a closed queue")
	HibernateException closedOperationQueue();

	@Message(id = 55, value = "Invalid URL given for configuration property '%1$s': %2$s; The specified resource could not be found.")
	HibernateException invalidConfigurationUrl(String propertyName, String url);

	@Message(id = 56, value = "Unable to load record for retrieval of generated properties; Entity type: %1$s, id: %2$s")
	HibernateException couldNotRetrieveEntityForRetrievalOfGeneratedProperties(String entityType, Serializable id);

	@Message(id = 57, value = "'%s' must not be null")
	IllegalArgumentException mustNotBeNull(String name);

	@Message(id = 58, value = "Parameter '%s' must not be null")
	IllegalArgumentException parameterMustNotBeNull(String parameterName);

	@Message(id = 59, value = "Unable to find a GridType for %s")
	HibernateException unableToFindGridType(String typeName);

	@LogMessage(level = WARN)
	@Message(id = 60, value = "Sequence id generator used for entity '%2$s' is not supported by grid dialect %1$s, falling back to table-based id generation. Consider to use @TableGenerator rather than @SequenceGenerator.")
	void dialectDoesNotSupportSequences(@FormatWith(ClassObjectFormatter.class) Class<?> dialectClass, String entityName);

	@LogMessage(level = WARN)
	@Message(id = 61, value = "The option '@TableGenerator#catalog()' is not supported by Hibernate OGM. Its value %s is going to be ignored.")
	void catalogOptionNotSupportedForTableGenerator(String catalogName);

	@LogMessage(level = WARN)
	@Message(id = 62, value = "The option '@TableGenerator#schema()' is not supported by Hibernate OGM. Its value %s is going to be ignored.")
	void schemaOptionNotSupportedForTableGenerator(String schemaName);

	@LogMessage(level = WARN)
	@Message(id = 63, value = "The option '@SequenceGenerator#catalog()' is not supported by Hibernate OGM. Its value %s is going to be ignored.")
	void catalogOptionNotSupportedForSequenceGenerator(String catalogName);

	@LogMessage(level = WARN)
	@Message(id = 64, value = "The option '@SequenceGenerator#schema()' is not supported by Hibernate OGM. Its value %s is going to be ignored.")
	void schemaOptionNotSupportedForSequenceGenerator(String schemaName);

	@Message(id = 65, value = "Id generation strategy IDENTITY configured for entity %1$s is not supported by the current grid dialect.")
	HibernateException getIdentityGenerationStrategyNotSupportedException(String entityName);

	@LogMessage(level = WARN)
	@Message(id = 66, value = "Entity type %s uses an optimistic locking strategy which is not supported by the "
			+ "current grid dialect in an atomic manner. There will be two datastore round-trips for version checking and updating the data.")
	void usingNonAtomicOptimisticLocking(String entityName);

	@Message(id = 67, value = "Trying to insert an already existing entity: %s")
	EntityAlreadyExistsException mustNotInsertSameEntityTwice(String primaryKey, @Cause Exception taee );

	@Message(id = 68, value = "Could not configure property %1$s#%2$s")
	HibernateException couldNotConfigureProperty(String entityName, String string, @Cause Exception e);

	@Message(id = 69, value = "Grid dialect %1$s does not support lock mode %2$s")
	HibernateException unsupportedLockMode(@FormatWith(ClassObjectFormatter.class) Class<? extends GridDialect> dialectClass, LockMode lockMode);

	@LogMessage(level = WARN)
	@Message(id = 70, value = "'%1$s' is no valid datastore provider short name. Valid values are: %2$s")
	void noValidDatastoreProviderShortName(String providerName, String validProviderNames);

	@Message(id = 71, value = "Unable to start datatore provider")
	ServiceException unableToStartDatastoreProvider(@Cause Exception e);

	@Message(id = 72, value = "Unable to configure datatore provider")
	ServiceException unableToConfigureDatastoreProvider(@Cause Exception e);

	@Message(id = 73, value = "Couldn't load the Lucene-based query parser backend. Make sure the dependency "
			+ "org.hibernate.hql:hibernate-hql-lucene is part of the classpath.")
	HibernateException cannotLoadLuceneParserBackend(@Cause Exception e);

	@LogMessage(level = INFO)
	@Message(id = 74, value = "Could not close session; swallowing exception as transaction completed")
	void unableToCloseSessionButSwallowingError( @Cause Exception e);

	@LogMessage(level = INFO)
	@Message(id = 75, value = "JBoss Transaction Manager (com.arjuna.ats.jta.TransactionManager) detected on classpath. Using JBossStandAloneJtaPlatform as JTAPlatform implementation")
	void jbossTransactionManagerDetected();

	@LogMessage(level = INFO)
	@Message(id = 76, value = "No explicit or implicit defined JTAPlatform. Using NoJtaPlatform")
	void noJtaPlatformDetected();

	@Message(id = 77, value = "Parameter '%s' must not be an empty string")
	IllegalArgumentException parameterSringMustNotBeEmpty(String parameterName);

}
