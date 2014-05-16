/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import org.jboss.logging.Logger;

/**
 * A factory class for class loggers. Allows a creation of loggers after the DRY principle.
 *
 * @author Hardy Ferentschik
 */
public class LoggerFactory {

	private static final CallerProvider callerProvider = new CallerProvider();

	public static Log make() {
		return Logger.getMessageLogger( Log.class, callerProvider.getCallerClass().getCanonicalName() );
	}

	private static class CallerProvider extends SecurityManager {

		public Class<?> getCallerClass() {
			return getClassContext()[2];
		}
	}
}
