/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.redis;

/**
 * @author Davide D'Alto
 */
class RedisTestProperties {

	public static final String ENVIRONMENT_REDIS_HOSTNAME = "REDIS_HOSTNAME";
	public static final String ENVIRONMENT_REDIS_PORT = "REDIS_PORT";
	public static final String ENVIRONMENT_REDIS_PASSWORD = "REDIS_PASSWORD";

	public static final String DEFAULT_HOSTNAME = "localhost";
	public static final String DEFAULT_PORT = "6379";

	private static String redisHost = initHostname() + ":" + initPort();
	private static String redisPassword = initPassword();

	private static String initHostname() {
		String redisHostName = System.getenv( ENVIRONMENT_REDIS_HOSTNAME );
		if ( isNull( redisHostName ) ) {
			redisHostName = DEFAULT_HOSTNAME;
		}
		return redisHostName;
	}

	private static String initPort() {
		String redisPortNumber = System.getenv( ENVIRONMENT_REDIS_PORT );
		if ( isNull( redisPortNumber ) ) {
			redisPortNumber = DEFAULT_PORT;
		}
		return redisPortNumber;
	}

	private static String initPassword() {
		String password = System.getenv( ENVIRONMENT_REDIS_PASSWORD );
		if ( isNull( password ) ) {
			return null;
		}
		return password;
	}

	private static boolean isNull(String value) {
		return value == null || value.length() == 0 || value.toLowerCase().equals( "null" );
	}

	public static String getHost() {
		return redisHost;
	}

	public static String getPassword() {
		return redisPassword;
	}
}
