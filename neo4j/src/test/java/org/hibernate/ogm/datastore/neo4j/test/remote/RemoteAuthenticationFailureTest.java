/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.remote;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.impl.DatastoreProviderType;
import org.hibernate.ogm.datastore.neo4j.remote.common.impl.RemoteNeo4jDatastoreProvider;
import org.hibernate.ogm.datastore.neo4j.utils.Neo4jTestHelper;
import org.hibernate.ogm.datastore.neo4j.utils.PropertiesReader;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Neo4j remote should throw proper exceptions when the credentials for authentication are wrong.
 *
 * @author Davide D'Alto
 */
public class RemoteAuthenticationFailureTest {

	private final Map<String, String> properties = new HashMap<String, String>( 2 );

	@BeforeClass
	public static void initEnvironment() {
		Neo4jTestHelper.initEnvironment();
	}

	@Before
	public void setup() {
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.HOST, properties );
		copyFromSystemPropertiesToLocalEnvironment( OgmProperties.PORT, properties );
		properties.putAll( PropertiesReader.getHibernateProperties() );
	}

	@Test
	public void testAuthenticationFailureAtStartUp() throws Exception {
		properties.put( OgmProperties.USERNAME, "completely wrong" );
		properties.put( OgmProperties.PASSWORD, "completely wrong" );

		DatastoreProviderType providerType = datastoreProvider( properties );

		// We only test with remote datastores
		if ( providerType != DatastoreProviderType.NEO4J_EMBEDDED ) {
			try {
				connectToRemoteDatastore( properties );
				fail( "Credentials should be invalid" );
			}
			catch (HibernateException e) {
				// Unable to start datastore provider
				assertThat( e.getMessage() ).startsWith( "OGM000071" );
				assertThat( e.getCause().getMessage() ).startsWith( "OGM001419" );
				assertThat( e.getCause().getMessage() ).contains( "Unauthorized" );
			}
		}
	}

	private static void connectToRemoteDatastore(Map<String, String> properties) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		DatastoreProviderType clazz = datastoreProvider( properties );

		RemoteNeo4jDatastoreProvider remoteDatastoreProvider = (RemoteNeo4jDatastoreProvider) ( Class.forName( clazz.getDatastoreProviderClassName() ) )
				.newInstance();
		remoteDatastoreProvider.configure( properties );
		try {
			remoteDatastoreProvider.start();
		}
		finally {
			remoteDatastoreProvider.stop();
		}
	}

	private static DatastoreProviderType datastoreProvider(Map<String, String> properties) {
		String provider = properties.get( OgmProperties.DATASTORE_PROVIDER );
		DatastoreProviderType clazz = DatastoreProviderType.byShortName( provider );
		return clazz;
	}

	private static void copyFromSystemPropertiesToLocalEnvironment(String environmentVariableName, Map<String, String> envProps) {
		String value = System.getProperties().getProperty( environmentVariableName );
		if ( value != null && value.length() > 0 ) {
			envProps.put( environmentVariableName, value );
		}
	}

}
