/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.logging.impl;

import org.hibernate.HibernateException;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@Message(id = 1701, value = "Cannot unmarshal key of type %1$s written by a newer version of Hibernate OGM."
			+ " Expecting version %3$s but found version %2$s.")
	HibernateException unexpectedKeyVersion(Class<?> clazz, int version, int supportedVersion);

	@LogMessage(level = Level.WARN)
	@Message(id = 1702, value = "Unknown cache '%s'. Creating new with default settings.")
	void unknownCache(String cacheName);

	@LogMessage(level = Level.INFO)
	@Message(id = 1703, value = "Ignite instance is stopped. Trying to restart" )
	void stoppedIgnite();

	@Message(id = 1704, value = "Cache '%s' not found")
	HibernateException cacheNotFound(String cacheName);

	@Message(id = 1705, value = "Invalid entity name '%s'")
	HibernateException invalidEntityName(String entityName);

	@Message(id = 1706, value = "Unsupported application server")
	UnsupportedOperationException unsupportedApplicationServer();

	@Message(id = 1707, value = "Invalid value for property '%1$s'. %2$s")
	HibernateException invalidPropertyValue(String propertyName, String message, @Cause Exception cause);
}
