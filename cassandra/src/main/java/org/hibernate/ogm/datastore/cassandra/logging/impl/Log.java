/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.logging.impl;

import static org.jboss.logging.Logger.Level.INFO;

import org.hibernate.HibernateException;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log message abstraction for i18n.
 *
 * @author Jonathan Halliday
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	// for cassandra backend use uniq ids up from 1601...

	@LogMessage(level = INFO)
	@Message(id = 1601, value = "Connecting to Cassandra at %1$s:%2$d")
	void connectingToCassandra(String host, int port);

	@LogMessage(level = INFO)
	@Message(id = 1602, value = "Closing connection to Cassandra")
	void disconnectingFromCassandra();

	@Message(id = 1603, value = "Unable to initialize Cassandra driver")
	HibernateException unableToInitializeCassandra(@Cause RuntimeException e);

	@Message(id = 1604, value = "Failed to create table %1$s")
	HibernateException failedToCreateTable(String table, @Cause RuntimeException e);

	@Message(id = 1605, value = "Failed to create index on table %1$s")
	HibernateException failedToCreateIndex(String table, @Cause RuntimeException e);

	@Message(id = 1606, value = "Failed to execute CQL operation %1$s")
	HibernateException failToExecuteCQL(String cqlStatement, @Cause RuntimeException e);

	@Message(id = 1607, value = "Failed to prepare CQL operation %1$s")
	HibernateException failToPrepareCQL(String cqlStatement, @Cause Throwable e);

	@Message(id = 1608, value = "Cannot create secondary index for index/key named '%2$s' with no columns for table '%1$s'")
	HibernateException indexWithNoColumns(String tableName, String name);

	@LogMessage(level = Level.WARN)
	@Message(id = 1609, value = "Cannot create multi-column secondary index for index/key named '%2$s' for table '%1$s'; Only considering first index column")
	void multiColumnIndexNotSupported(String tableName, String name);
}
