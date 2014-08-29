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

	@Message(id = 1401, value = "Cannot generate sequence %s")
	HibernateException cannotGenerateSequence(String sequenceName);

	@LogMessage(level = DEBUG)
	@Message(id = 1402, value = "An error occured while generating the sequence %s")
	void errorGeneratingSequence(String sequenceName, @Cause Exception e);

	@Message(id = 1403, value = "TransactionManager not found")
	HibernateException transactionManagerNotFound();

	@Message(id = 1404, value = "Error during transaction recovery")
	HibernateException errorDuringRecovery(@Cause Exception t);

	@Message(id = 1405, value = "Error reading transaction state")
	HibernateException errorReadingTransactionState(@Cause Exception e);

	@Message(id = 1406, value = "Error adding unique constraint")
	HibernateException errorAddingUniqueConstraint(@Cause Exception e);

	@Message(id = 1407, value = "Error adding sequence")
	HibernateException errorAddingSequence(@Cause Exception e);

	@Message(id = 1408, value = "Error incrementing sequence value")
	HibernateException errorIncrementingSequenceValue(@Cause Exception e);
}
