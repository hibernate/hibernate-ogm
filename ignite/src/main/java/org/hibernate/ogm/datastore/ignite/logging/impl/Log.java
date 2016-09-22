/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.logging.impl;

import org.apache.ignite.IgniteException;
import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.ignite.exception.IgniteHibernateException;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@Message(id = 1501, value = "Cannot unmarshal key of type %1$s written by a newer version of Hibernate OGM."
			+ " Expecting version %3$s but found version %2$s.")
	HibernateException unexpectedKeyVersion(Class<?> clazz, int version, int supportedVersion);

	@LogMessage(level = Level.WARN)
	@Message(id = 1502, value = "Unknown cache '%s'. Creating new with default settings.")
	void unknownCache(String cacheName);

	@LogMessage(level = Level.INFO)
	@Message(id = 1503, value = "Ignite instance is stopped. Trying to restart" )
	void stoppedIgnite();

	@Message(id = 1504, value = "Cache '%s' not found")
	IgniteException cacheNotFound(String cacheName);

	@Message(id = 1505, value = "Invalid entity name '%s'")
	IgniteHibernateException invalidEntityName(String entityName);

	@Message(id = 1506, value = "Unsupported application server")
	UnsupportedOperationException unsupportedApplicationServer();
}
