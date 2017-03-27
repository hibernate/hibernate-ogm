/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.logging.impl;

import org.hibernate.HibernateException;
import org.hibernate.service.spi.ServiceException;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages and exceptions of the Infinispan dialect.
 *
 * @author Gunnar Morling
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@Message(id = 1101, value = "Cannot unmarshal key of type %1$s written by a newer version of Hibernate OGM."
			+ " Expecting version %3$s but found version %2$s.")
	HibernateException unexpectedKeyVersion(Class<?> clazz, int version, int supportedVersion);

	@Message(id = 1102, value = "Unable to find or initialize Infinispan CacheManager")
	ServiceException unableToInitializeInfinispan(@Cause RuntimeException e);

	@Message(id = 1103, value = "Infinispan Externalizer having id [%d] not registered in CacheManager. " +
			"This Externalizer is required and included in Hibernate OGM as '%2$s': if you provide a CacheManager make sure it can" +
			"auto-discover extension points from Hibernate OGM before starting.")
	HibernateException externalizersNotRegistered(Integer externalizerId, Class<?> aClass);

	@Message(id = 1104, value = "Infinispan Externalizer '%s' was registered but apparently loaded from the " +
			"wrong module. Aborting as a version mismatch could corrupt stored data.")
	HibernateException registeredExternalizerNotLoadedFromOGMClassloader(Class<? extends AdvancedExternalizer> aClass);

	@Message(id = 1105, value = "Infinispan Externalizer mistmatch: id [%1$d] was registered but taken " +
			"by implementation '%2$s'. Expected externalizer: '%3$s' ")
	HibernateException externalizerIdNotMatchingType(Integer externalizerId, AdvancedExternalizer<?> registeredExternalizer, AdvancedExternalizer expectedExternalizer);
}
