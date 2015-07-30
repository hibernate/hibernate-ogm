/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.logging.impl;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.OgmProperties;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.INFO;

/**
 * Log messages and exceptions of the Redis dialect.
 *
 * @author Mark Paluch
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@LogMessage(level = INFO)
	@Message(id = 1701, value = "Connecting to Redis at %1$s with a timeout set at %2$d millisecond(s)")
	void connectingToRedis(String host, long timeout);

	@LogMessage(level = INFO)
	@Message(id = 1702, value = "Closing connection to Redis")
	void disconnectingFromRedis();

	@Message(id = 1703, value = "Unable to find or initialize a connection to the Redis server")
	HibernateException unableToInitializeRedis(@Cause RuntimeException e);

	@Message(id = 1704, value = "The value set for the configuration property '" + OgmProperties.DATABASE + "' must be a number between 0 and 15. Found '%s'.")
	HibernateException illegalDatabaseValue(int value);
}
