/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.remote;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.neo4j.remote.impl.RemoteNeo4jDatastoreProvider;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.SkippableTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Neo4j remote should throw proper exceptions when the credentials for authentication are wrong.
 *
 * @author Davide D'Alto
 */
@RunWith(SkippableTestRunner.class)
@SkipByGridDialect(value = { GridDialectType.NEO4J }, comment = "We need a remote server")
public class RemoteAuthenticationFailureTest {

	@Test
	public void testAuthenticationFailureAtStartUp() throws Exception {
		Properties properties = new Properties();
		properties.setProperty( OgmProperties.USERNAME, "completely wrong" );
		properties.setProperty( OgmProperties.PASSWORD, "completely wrong" );
		RemoteNeo4jDatastoreProvider remoteDatastoreProvider = new RemoteNeo4jDatastoreProvider();
		remoteDatastoreProvider.configure( properties );
		try {
			remoteDatastoreProvider.start();
			fail( "Credentials should be invalid" );
		}
		catch (HibernateException e) {
			// Unable to start datastore provider
			assertThat( e.getMessage() ).startsWith( "OGM000071" );
			assertThat( e.getCause().getMessage() ).startsWith( "OGM001419" );
		}
		finally {
			remoteDatastoreProvider.stop();
		}
	}
}
