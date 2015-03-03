/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.neo4j.impl;

import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.util.StringLogger;
import org.neo4j.kernel.logging.LogMarker;

import org.hibernate.ogm.datastore.neo4j.logging.impl.Log;
import org.hibernate.ogm.datastore.neo4j.logging.impl.LoggerFactory;

/**
 * Implements Neo4J's StringLogger and write the content to JBoss Logging.
 * Regular messages are sent as debug, user messages as info, exception messages as error
 * and long messages as separated calls to logMessage.
 * This follows Neo4J's {@code LogbackService} implementation.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class StringLoggerToJBossLoggingAdaptor extends StringLogger {
	public static final StringLogger JBOSS_LOGGING_STRING_LOGGER = new StringLoggerToJBossLoggingAdaptor();
	private static final Log log = LoggerFactory.getLogger();

	@Override
	public void logLongMessage(String msg, Visitor<LineLogger, RuntimeException> source, final boolean flush) {
		// copy of Neo4J's Slf4J / Logback adapter logic
		logMessage( msg, flush );
		source.visit(
				new LineLogger() {
					@Override
					public void logLine(String line) {
						logMessage( line, flush );
					}
				} );

	}

	@Override
	public void logMessage(String msg, boolean flush) {
		log.logNeo4JQueryEngineMessage( msg );
	}

	@Override
	public void logMessage(String msg, LogMarker marker) {
		// the only LogMarker I could find is the "console" marker that is aimed at user messages
		log.logNeo4JQueryEngineUserMessage( marker.getName(), msg );
	}

	@Override
	public void logMessage(String msg, Throwable cause, boolean flush) {
		log.logNeo4JQueryEngineException( msg, cause );
	}

	@Override
	public void addRotationListener(Runnable listener) {
		// nothing to do
	}

	@Override
	public void flush() {
		// nothing to do
	}

	@Override
	public void close() {
		// nothing to do
	}

	@Override
	public boolean isDebugEnabled() {
		return log.isDebugEnabled();
	}

	@Override
	protected void logLine(String line) {
		log.logNeo4JQueryEngineMessage( line );
	}
}
