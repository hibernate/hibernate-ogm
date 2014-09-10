/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.logging.impl;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

import org.hibernate.HibernateException;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.TupleOperation;
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

	@Message(id = 1403, value = "Constraint violation for entity %s: %s")
	HibernateException constraintViolation(EntityKey entityKey, TupleOperation operation, @Cause Exception cause);

	@LogMessage(level = WARN)
	@Message(id = 1404, value = "Neo4j does not support constraints spanning multiple columns. Unique key %1$s for %2$s on columns %3$s cannot be created")
	void constraintSpanningMultipleColumns(String name, String tableName, String columns);
}
