/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.logging.impl;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.text.ParseException;

import javax.persistence.OptimisticLockException;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	String ERROR_DESCRIPTION = "HTTP response status code: %03d, error: '%s', reason: '%s'";

	@Message(id = 1301, value = "An error occurred increasing the value of the key")
	HibernateException errorCalculatingNextValue(@Cause Exception exception);

	@Message(id = 1302, value = "An error occurred creating CouchDB Document, " + ERROR_DESCRIPTION)
	HibernateException errorCreatingDocument(int status, String error, String cause);

	@Message(id = 1303, value = "An error occurred deleting CouchDB Document, " + ERROR_DESCRIPTION)
	HibernateException errorDeletingDocument(int status, String error, String reason);

	@Message(id = 1304, value = "Unable to connect to CouchDB")
	HibernateException couchDBConnectionProblem(@Cause Exception exception);

	@Message(id = 1305, value = "An error occurred dropping the database, " + ERROR_DESCRIPTION)
	HibernateException errorDroppingDatabase(int status, String error, String reason);

	@Message(id = 1306, value = "An error occurred retrieving entity with id %s, " + ERROR_DESCRIPTION)
	HibernateException errorRetrievingEntity(String entityId, int status, String error, String reason);

	@Message(id = 1307, value = "An error occurred retrieving association with id %s, " + ERROR_DESCRIPTION)
	HibernateException errorRetrievingAssociation(String id, int status, String error, String reason);

	@Message(id = 1308, value = "An error occurred retrieving the number of associations stored in CouchDB, " + ERROR_DESCRIPTION)
	HibernateException unableToRetrieveTheNumberOfAssociations(int status, String error, String reason);

	@Message(id = 1309, value = "An error occurred retrieving the number of entities stored in CouchDB, " + ERROR_DESCRIPTION)
	HibernateException unableToRetrieveTheNumberOfEntities(int status, String error, String reason);

	@Message(id = 1310, value = "An error occurred retrieving tuples for table %s, " + ERROR_DESCRIPTION)
	HibernateException unableToRetrieveTheTupleByEntityKeyMetadata(String tableName, int status, String error, String reason);

	@Message(id = 1311, value = "An error occurred retrieving a key value, " + ERROR_DESCRIPTION)
	HibernateException errorRetrievingKeyValue(int status, String error, String reason);

	@Message(id = 1312, value = "An error occurred retrieving the list of databases, " + ERROR_DESCRIPTION)
	HibernateException unableToRetrieveTheListOfDatabase(int status, String error, String reason);

	@Message(id = 1313, value = "An error occurred retrieving database %s, " + ERROR_DESCRIPTION)
	HibernateException errorCreatingDatabase(String dataBaseName, int status, String error, String reason);

	@LogMessage(level = INFO)
	@Message(id = 1314, value = "Connecting to CouchDB at %s")
	void connectingToCouchDB(String database);

	@Message(id = 1315, value = "An error occurred, malformed database URL. Database host: %s, database port: %03d, database name: %s")
	HibernateException malformedDataBaseUrl(@Cause Exception e, String databaseHost, int databasePort,
			String databaseName);

	@Message(id = 1317, value = "Could not parse date string: %s")
	HibernateException errorParsingStringToDate(@Cause ParseException pe, String date);

	@Message(id = 1318, value = "Error shutting down the datastore")
	HibernateException shutDownDatastoreException(@Cause Exception e);

	@Message(id = 1319, value = "An error occurred when retrieving the current revision of entity with id %s, " + ERROR_DESCRIPTION)
	HibernateException errorRetrievingCurrentRevision(String entityId, int status, String error, String reason);

	@Message(id = 1320, value = "The document with id %s has been concurrently modified.")
	OptimisticLockException getDocumentHasBeenConcurrentlyModifiedException(String id);

	@Message(id = 1321, value = "Database %s does not exist. Either create it yourself or set property '" + OgmProperties.CREATE_DATABASE + "' to true.")
	HibernateException databaseDoesNotExistException(String databaseName);

	@LogMessage(level = WARN)
	@Message(id = 1322, value = "Entity '%s' does not have a revision property; In order to make use of CouchDB's "
			+ "built-in optimistic locking mechanism, it is recommended to define a property '@Generated @Version String _rev'.")
	void entityShouldHaveRevisionProperty(String entityName);
}
