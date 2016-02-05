package org.hibernate.ogm.datastore.ignite.logging.impl;

import org.hibernate.HibernateException;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@Message(id = 1501, value = "Cannot unmarshal key of type %1$s written by a newer version of Hibernate OGM."
			+ " Expecting version %3$s but found version %2$s.")
	HibernateException unexpectedKeyVersion(Class<?> clazz, int version, int supportedVersion);
	
}
