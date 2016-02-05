package org.hibernate.ogm.datastore.ignite.logging.impl;

/**
 * Factory for obtaining {@link Log} instances.
 * 
 * @author Dmitriy Kozlov
 *
 */
public class LoggerFactory {

	private static final CallerProvider callerProvider = new CallerProvider();

	public static Log getLogger() {
		return org.jboss.logging.Logger.getMessageLogger(Log.class, callerProvider.getCallerClass().getCanonicalName());
	}
	
	private static class CallerProvider extends SecurityManager {

		public Class<?> getCallerClass() {
			return getClassContext()[2];
		}
	}
}
