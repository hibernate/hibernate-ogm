/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.test.dialect.authenticated;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.ogm.utils.TestHelper;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests the access of a CouchDB database which requires authentication. The test assumes a database in the default
 * configuration (i.e. without authentication enabled) and does the following preparation steps:
 * <ul>
 * <li>create a server admin user</li>
 * <li>create a test database</li>
 * <li>create an admin user for that database (db admin rights are required to create views)</li>
 * </ul>
 * The actual test is then run using the credentials of the db admin user, followed by removing the created users and
 * database.
 *
 * @author Gunnar Morling
 */
public class AuthenticatedAccessTest {

	private static String host;
	private static int port;
	private static String serverUri;

	private static String serverAdminUser = "alfred";
	private static String serverAdminPassword = "secret";

	private static String databaseUser = "cary";
	private static String databaseUserPassword = "not_so_secret";
	private static String database = "testdb_authenticated";
	private static String databaseUserRevision;

	private static SessionFactory sessions;
	private static ResteasyClient client;

	@BeforeClass
	public static void createDatabaseAndSetUpAuthentication() throws Exception {
		OgmConfiguration configuration = getConfiguration();

		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configuration );
		host = propertyReader.property( OgmProperties.HOST, String.class ).withDefault( "localhost" ).getValue();
		port = propertyReader.property( OgmProperties.PORT, int.class ).withDefault( 5984 ).getValue();
		serverUri = "http://" + host + ":" + port;

		client = getClientWithServerAdminCredentials();

		createServerAdminUser();
		createTestDatabase();
		createDatabaseUser();

		sessions = configuration.buildSessionFactory();
	}

	@AfterClass
	public static void dropDatabaseAndDeleteAdminUser() throws Exception {
		closeSessionFactory();
		deleteDatabaseUser();
		dropTestDatabase();
		deleteServerAdminUser();
	}

	@Test
	public void databaseAccessWithCredentialsShouldSucceed() {
		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		Flower flower = new Flower();
		flower.setId( "1" );
		flower.setName( "Caltha palustris" );

		session.persist( flower );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();

		Flower loadedFlower = (Flower) session.get( Flower.class, flower.getId() );
		assertNotNull( loadedFlower );
		assertEquals( flower.getName(), loadedFlower.getName() );

		transaction.commit();
		session.clear();
		session.close();
	}

	private static OgmConfiguration getConfiguration() {
		OgmConfiguration configuration = TestHelper.getDefaultTestConfiguration( Flower.class );
		configuration.setProperty( OgmProperties.DATABASE, database );
		configuration.setProperty( OgmProperties.USERNAME, databaseUser );
		configuration.setProperty( OgmProperties.PASSWORD, databaseUserPassword );
		configuration.setProperty( OgmProperties.CREATE_DATABASE, Boolean.FALSE.toString() );

		return configuration;
	}

	private static void createServerAdminUser() {
		Client client = ClientBuilder.newClient();
		WebTarget target = client.target( serverUri + "/_config/admins/" + serverAdminUser );
		target.request().put( Entity.text( "\"" + serverAdminPassword + "\"" ) ).close();
	}

	private static void createDatabaseUser() throws Exception {
		// 1.) create user
		WebTarget target = client.target( serverUri + "/_users/org.couchdb.user:" + databaseUser );
		Response response = target.request().put( Entity.text(
				"{ " +
						"\"_id\" : \"org.couchdb.user:" + databaseUser + "\", " +
						"\"type\" : \"user\", " +
						"\"name\" : \"" + databaseUser + "\", " +
						"\"roles\" : [], " +
						"\"password\" : \"" + databaseUserPassword + "\"" +
				" }" )
				);

		String entity = response.readEntity( String.class );
		databaseUserRevision = (String) new ObjectMapper().readValue( entity, Map.class ).get( "rev" );
		response.close();

		// 2.) make the user an admin of the database
		target = client.target( serverUri + "/" + database + "/_security" );
		response = target.request().put( Entity.text(
				"{" +
						"\"admins\": {" +
								"\"names\":[\"" + databaseUser + "\"]," +
								" \"roles\":[]" +
						"}, " +
						"\"readers\": {" +
								"\"names\":[]," +
								"\"roles\":[]" +
						"}" +
				"}"
			)
		);

		response.close();
	}

	private static void createTestDatabase() {
		WebTarget target = client.target( serverUri + "/" + database );
		target.request().put( null ).close();
	}

	private static void deleteDatabaseUser() {
		WebTarget target = client.target( serverUri + "/_users/org.couchdb.user:" + databaseUser + "?rev=" + databaseUserRevision );
		target.request().delete().close();
	}

	private static void dropTestDatabase() {
		WebTarget target = client.target( serverUri + "/" + database );
		target.request().delete().close();
	}

	private static void deleteServerAdminUser() {
		WebTarget target = client.target( serverUri + "/_config/admins/" + serverAdminUser );
		target.request().delete().close();
	}

	private static ResteasyClient getClientWithServerAdminCredentials() {
		DefaultHttpClient httpClient = new DefaultHttpClient();

		httpClient.getCredentialsProvider().setCredentials(
				new AuthScope( host, port ),
				new UsernamePasswordCredentials( serverAdminUser, serverAdminPassword )
				);

		AuthCache authCache = new BasicAuthCache();
		authCache.put(
				new HttpHost( host, port, "http" ),
				new BasicScheme()
				);

		BasicHttpContext localContext = new BasicHttpContext();
		localContext.setAttribute( ClientContext.AUTH_CACHE, authCache );

		return new ResteasyClientBuilder()
		.httpEngine( new ApacheHttpClient4Engine( httpClient, localContext ) )
		.build();
	}

	private static void closeSessionFactory() {
		if ( sessions != null ) {
			sessions.close();
		}
	}

}
