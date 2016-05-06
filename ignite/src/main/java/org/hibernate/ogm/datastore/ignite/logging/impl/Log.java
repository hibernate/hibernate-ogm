/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.logging.impl;

import org.hibernate.HibernateException;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@Message(id = 1701, value = "Cannot unmarshal key of type %1$s written by a newer version of Hibernate OGM."
			+ " Expecting version %3$s but found version %2$s.")
	HibernateException unexpectedKeyVersion(Class<?> clazz, int version, int supportedVersion);

	@Message(id = 1702, value = "Invalid entity name: %1$s")
	HibernateException invalidEntityName(String entityType);

	@Message(id = 1703, value = "Cache '%1$s' not found")
	HibernateException cacheNotFound(String table);

	@Message(id = 1704, value = "Exception acquiring lock on obejct '%1$s'")
	HibernateException exceptionAcquiringLock(String object, @Cause Exception exception);

}
