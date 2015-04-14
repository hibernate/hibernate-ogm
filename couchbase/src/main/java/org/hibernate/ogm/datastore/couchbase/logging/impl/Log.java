/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchbase.logging.impl;
import static org.jboss.logging.Logger.Level.INFO;

import org.hibernate.HibernateException;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Stawicka Ewa
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {
	@LogMessage(level = INFO)
	@Message(id = 1701, value = "Connecting to CouchBase at %s")
	void connectingToCouchBase(String database);

	@Message(id = 1702, value = "Unable to connect to CouchBase")
	HibernateException couchBaseConnectionProblem(@Cause Exception exception);

	@Message(id = 1703, value = "Unable to read response and create EntityDocument")
	HibernateException failedToReadResponse(@Cause Exception exception);

	@Message(id = 1704, value = "Unable to write EntityDocument to Json")
	HibernateException failedToWriteDocument(@Cause Exception e);

	@Message(id = 1705, value = "An error occurred, malformed database URL. Database host: %s, database port: %03d, bucket name: %s")
	HibernateException malformedDataBaseUrl(@Cause Exception e, String host, int port, String bucketName);
}
