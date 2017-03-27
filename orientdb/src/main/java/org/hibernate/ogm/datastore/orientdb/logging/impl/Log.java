/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.logging.impl;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.orientdb.OrientDBProperties.StorageModeEnum;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Description of errors and messages, that can be throw by the module
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@Message(id = 1700, value = "Cannot use field %s in  class %s! It is unsupported field!")
	HibernateException cannotUseInEntityUnsupportedSystemField(String fieldName, String className);

	@Message(id = 1701, value = "Cannot execute query %s!")
	HibernateException cannotExecuteQuery(String propertyQuery, @Cause Exception cause);

	@Message(id = 1702, value = "Cannot use unsupported type %s!")
	HibernateException cannotUseUnsupportedType(@SuppressWarnings("rawtypes") Class type);

	@Message(id = 1703, value = "Cannot create database %s !")
	HibernateException cannotCreateDatabase(String database, @Cause Exception cause);

	@Message(id = 1704, value = "Unsupported storage type %s !")
	HibernateException unsupportedStorage(StorageModeEnum storage);

	@Message(id = 1705, value = "User not defined! Please set '%s' property!")
	HibernateException userNotDefined(String propertyName);

	@Message(id = 1706, value = "Password not defined! Please set '%s' property!")
	HibernateException passwordNotDefined(String propertyName);

	@Message(id = 1707, value = "Cannot parse query %s!")
	HibernateException cannotParseQuery(String propertyQuery, @Cause Exception cause);
}
