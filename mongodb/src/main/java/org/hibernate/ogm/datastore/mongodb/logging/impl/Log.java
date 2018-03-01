/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.logging.impl;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.util.impl.ClassObjectFormatter;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import com.mongodb.MongoException;

/**
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@LogMessage(level = INFO)
	@Message(id = 1201, value = "Connecting to MongoDB at %1$s with a timeout set at %2$d millisecond(s)")
	void connectingToMongo(String host, int timeout);

	@LogMessage(level = INFO)
	@Message(id = 1202, value = "Closing connection to MongoDB")
	void disconnectingFromMongo();

	@Message(id = 1203, value = "Unable to find or initialize a connection to the MongoDB server")
	HibernateException unableToInitializeMongoDB(@Cause RuntimeException e);

	@LogMessage(level = INFO)
	@Message(id = 1206, value = "Mongo database named [%s] is not defined. Creating it!")
	void creatingDatabase(String dbName);

	@LogMessage(level = INFO)
	@Message(id = 1207, value = "Connecting to Mongo database named [%s].")
	void connectingToMongoDatabase(String dbName);

	@Message(id = 1209, value = "The database named [%s] cannot be dropped")
	HibernateException unableToDropDatabase(@Cause MongoException e, String databaseName);

	@LogMessage(level = TRACE)
	@Message(id = 1210, value = "Removed [%d] associations")
	void removedAssociation(long nAffected);

	@Message(id = 1214, value = "Unable to connect to MongoDB instance: %1$s")
	HibernateException unableToConnectToDatastore(String message, @Cause Exception e);

	@Message(id = 1217, value = "The following native query does neither specify the collection name nor is its result type mapped to an entity: %s")
	HibernateException unableToDetermineCollectionName(String nativeQuery);

	@LogMessage(level = WARN)
	@Message(id = 1218, value = "Cannot use primary key column name '%s' for id generator, going to use '%s' instead")
	void cannotUseGivenPrimaryKeyColumnName(String givenKeyColumnName, String usedKeyColumnName);

	@Message(id = 1219, value = "Database %s does not exist. Either create it yourself or set property '" + OgmProperties.CREATE_DATABASE + "' to true.")
	HibernateException databaseDoesNotExistException(String databaseName);

	// The following statements have to return MappingException to make sure Hibernate ORM doesn't wrap them in a
	// generic failure
	// but maintains the user friendly error message

	@Message(id = 1220, value = "When using MongoDB it is not valid to use a name for a table (a collection) which starts with the 'system.' prefix."
			+ " Please change name for '%s', for example by using @Table ")
	MappingException collectionNameHasInvalidSystemPrefix(String qualifiedName);

	@Message(id = 1221, value = "When using MongoDB it is not valid to use a name for a table (a collection) which contains the NUL character '\\0'."
			+ " Please change name for '%s', for example by using @Table ")
	MappingException collectionNameContainsNULCharacter(String qualifiedName);

	@Message(id = 1222, value = "When using MongoDB it is not valid to use a name for a table (a collection) which contains the dollar character '$';"
			+ " for example this is a common problem with inner classes." + " Please pick a valid collection name for '%s', for example by using @Table ")
	MappingException collectionNameContainsDollarCharacter(String qualifiedName);

	@Message(id = 1223, value = "When using MongoDB it is not valid to use a field name which starts with the prefix '$'."
			+ " Please change name for '%s', for example by using @Column ")
	MappingException fieldNameHasInvalidDollarPrefix(String columnName);

	@Message(id = 1224, value = "When using MongoDB it is not valid to use a field name which contains the NUL character '\\0'."
			+ " Please change name for '%s', for example by using @Column ")
	MappingException fieldNameContainsNULCharacter(String fieldName);

	@Message(id = 1225, value = "This WriteConcern has been deprecated or removed by MongoDB: %s")
	HibernateException writeConcernDeprecated(String writeConcern);

	@Message(id = 1226, value = "Unable to use reflection on invoke method '%s#%s' via reflection.")
	HibernateException unableToInvokeMethodViaReflection(String clazz, String method);

	@Message(id = 1227, value = "Query must be executed using the 'executeUpdate()' method: %s")
	HibernateException updateQueryMustBeExecutedViaExecuteUpdate(MongoDBQueryDescriptor queryDescriptor);

	@Message(id = 1228, value = "Query must be executed using 'getResultList()' or 'getSingleResult()' method: %s")
	HibernateException readQueryMustBeExecutedViaGetResultList(MongoDBQueryDescriptor queryDescriptor);

	@Message(id = 1229, value = "Constraint violation for entity %s (%s)")
	HibernateException constraintViolationForEntity(EntityKey entityKey, String message, @Cause Exception cause);

	@Message(id = 1230, value = "Constraint violation while flushing several entities (%s)")
	HibernateException constraintViolationOnFlush(String message, @Cause Exception cause);

	@Message(id = 1231, value = "Unable to create index %2$s on collection %1$s")
	HibernateException unableToCreateIndex(String collection, String indexName, @Cause Exception e);

	@Message(id = 1232, value = "Unable to create text index %2$s on collection %1$s. A text index named %3$s already exists and MongoDB only supports one text index per collection.")
	HibernateException unableToCreateTextIndex(String collection, String newIndexName, String existingIndexName);

	@LogMessage(level = ERROR)
	@Message(id = 1233, value = "Cannot create an index with an empty name for collection %1$s. Please provide a name for all the indexes.")
	void indexNameIsEmpty(String collection);

	@LogMessage(level = ERROR)
	@Message(id = 1234, value = "No valid keys found for the index %2$s of collection %1$s.")
	void noValidKeysForIndex(String collection, String indexName);

	@LogMessage(level = WARN)
	@Message(id = 1235, value = "Index option for index %2$s of collection %1$s are referencing a non existing index.")
	void indexOptionReferencingNonExistingIndex(String collection, String forIndex);

	@Message(id = 1236, value = "The options for index %2$s of collection %1$s are not a valid JSON object.")
	HibernateException invalidOptionsFormatForIndex(String collection, String indexName, @Cause Exception e);

	@Message(id = 1237, value = "Invalid GeoJSON type %1$s. Expecting %2$s.")
	HibernateException invalidGeoJsonType(String actualType, String expectedType);

	@Message(id = 1238, value = "Unable to execute command \"%s\". Error message : %s. Code name: %s")
	HibernateException unableToExecuteCommand(String command, String errorMessage,String codeMessage, @Cause Exception e);

	@Message(id = 1239, value = "Dialect %s does not support named parameters when calling stored procedures")
	HibernateException dialectDoesNotSupportNamedParametersForStoredProcedures( @FormatWith(ClassObjectFormatter.class) Class<?> dialectClass );

	@Message(id = 1240, value = "Procedures returning muliple documents are not supported. Procedure '%1$s' returned %2$d results")
	HibernateException multipleDocumentReturnedByStoredProcedure(String storedProcedureName, int size);
}
