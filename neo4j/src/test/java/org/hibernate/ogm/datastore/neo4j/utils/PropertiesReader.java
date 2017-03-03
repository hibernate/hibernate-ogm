/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Reads properties from the hibernate.properties configuration for the tests.
 *
 * @author Davide D'Alto
 */
public final class PropertiesReader {

	private static final Map<String, String> hibernateProperties = readProperties();

	private static Map<String, String> readProperties() {
		try {
			Properties hibProperties = new Properties();
			try ( InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream( "hibernate.properties" ) ) {
				hibProperties.load( resourceAsStream );
			}
			Map<String, String> props = new HashMap<>();
			for ( Map.Entry<Object, Object> entry : hibProperties.entrySet() ) {
				props.put( String.valueOf( entry.getKey() ), String.valueOf( entry.getValue() ) );
			}
			return Collections.unmodifiableMap( props );
		}
		catch (IOException e) {
			throw new RuntimeException( "Missing properties file: hibernate.properties" );
		}
	}

	public static Map<String, String> getHibernateProperties() {
		return hibernateProperties;
	}
}
