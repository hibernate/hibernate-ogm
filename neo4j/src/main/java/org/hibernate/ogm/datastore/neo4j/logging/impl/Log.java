/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.logging.impl;

import static org.jboss.logging.Logger.Level.DEBUG;

import org.hibernate.HibernateException;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * @author Davide D'Alto
 */
@MessageLogger(projectCode = "OGM")
public interface Log extends org.hibernate.ogm.util.impl.Log {

	@Message(id = 1401, value = "Cannot generate sequence")
	HibernateException cannotGenerateSequence();

	@LogMessage(level = DEBUG)
	@Message(id = 1402, value = "An error occured while generating athe next value of a sequence")
	void errorGeneratingSequence(@Cause Exception e);
}
