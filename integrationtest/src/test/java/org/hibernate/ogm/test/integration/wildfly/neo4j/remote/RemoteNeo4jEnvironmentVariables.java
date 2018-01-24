/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.neo4j.remote;

import org.hibernate.ogm.datastore.neo4j.remote.common.impl.RemoteNeo4jConfiguration;

/**
 * @author Davide D'Alto
 */
public class RemoteNeo4jEnvironmentVariables {

	private static final String USERNAME = "NEO4J_USERNAME";
	private static final String PASSWORD = "NEO4J_PASSWORD";
	private static final String HOSTNAME = "NEO4J_HOSTNAME";
	private static final String PORT = "NEO4J_PORT";

	private static String neo4jHostName;
	private static String neo4jPortNumber;

	static {
		setHostName();
		setPortNumber();
	}

	private static void setHostName() {
		neo4jHostName = System.getenv( HOSTNAME );
		if ( isNull( neo4jHostName ) ) {
			neo4jHostName = "localhost";
		}
	}

	private static void setPortNumber() {
		neo4jPortNumber = System.getenv( PORT );
		if ( isNull( neo4jPortNumber ) ) {
			neo4jPortNumber = String.valueOf( RemoteNeo4jConfiguration.DEFAULT_HTTP_PORT );
		}
	}

	private static boolean isNull(String value) {
		return value == null || value.length() == 0 || value.toLowerCase().equals( "null" );
	}

	public static String getNeo4jHost() {
		return neo4jHostName + ":" + neo4jPortNumber;
	}

	public static String getNeo4jHostWithPort(int port) {
		return neo4jHostName + ":" + port;
	}

	public static String getNeo4jUsername() {
		return System.getenv( USERNAME );
	}

	public static String getNeo4jPassword() {
		return System.getenv( PASSWORD );
	}
}
