/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.test.dialect.backend.impl;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper.environmentProperties;
import static org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper.initEnvironmentProperties;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Properties;

import javax.persistence.OptimisticLockException;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.impl.CouchDBDatastore;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;
import org.hibernate.ogm.datastore.couchdb.test.dialect.CouchDBDialectTest;
import org.hibernate.ogm.datastore.couchdb.util.impl.DatabaseIdentifier;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
public class CouchDBDatastoreTest {

	public static final int DEFAULT_PORT = 5984;
	public static final String LOCALHOST = "localhost";
	private CouchDBDatastore dataStore;
	private Properties properties;
	private boolean dataBaseDropped;

	static {
		initEnvironmentProperties();
	}

	@Before
	public void setUp() throws Exception {
		setUpDatastore();
	}

	@After
	public void tearDown() throws Exception {
		dropDatabase();
	}

	private void dropDatabase() {
		if ( dataStore != null && !dataBaseDropped ) {
			dataStore.dropDatabase();
			dataBaseDropped = true;
		}
	}

	@Test(expected = OptimisticLockException.class)
	public void testUpdatingADocumentWithWrongRevisionNumber() {
		Document createdDocument = dataStore.saveDocument( createEntity() );
		String firstVersion = createdDocument.getRevision();

		createdDocument = dataStore.saveDocument( createdDocument );
		createdDocument.setRevision( firstVersion );

		dataStore.saveDocument( createdDocument );
	}

	@Test(expected = HibernateException.class)
	public void testCreatingADocumentAfterDeletingDatabase() {
		dropDatabase();

		dataStore.saveDocument( createEntity() );
	}

	@Test(expected = HibernateException.class)
	public void testCreatingInstanceWithUnavaibleHost() throws Exception {
		dataStore = null;
		CouchDBDatastore.newInstance( getDatabaseIdentifierWithUnavailableHost(), true );
	}

	@Test
	public void testDeleteADocument() {
		Document createdDocument = dataStore.saveDocument( createEntity() );
		String createdDocumentId = createdDocument.getId();
		dataStore.deleteDocument( createdDocument.getId(), createdDocument.getRevision() );

		EntityDocument entity = dataStore.getEntity( createdDocumentId );

		assertThat( entity, nullValue() );
	}

	@Test(expected = OptimisticLockException.class)
	public void testDeleteADocumentWithWrongRevision() {
		Document createdDocument = dataStore.saveDocument( createEntity() );
		final String revisionBeforeUpdate = createdDocument.getRevision();

		// saving the document will change its revision value
		dataStore.saveDocument( createdDocument );
		createdDocument.setRevision( revisionBeforeUpdate );

		dataStore.deleteDocument( createdDocument.getId(), createdDocument.getRevision() );
	}

	@Test
	public void testGetEntity() {
		Document createdDocument = dataStore.saveDocument( createEntity() );
		EntityDocument entity = dataStore.getEntity( createdDocument.getId() );
		assertThat( entity, notNullValue() );
	}

	@Test
	public void testGetEntityWithWrongIdReturnNullValue() {
		Document createdDocument = dataStore.saveDocument( createEntity() );
		EntityDocument entity = dataStore.getEntity( createdDocument.getId() + "_1" );
		assertThat( entity, nullValue() );
	}

	private void setUpDatastore() throws Exception {
		dataBaseDropped = false;
		loadProperties();
		Object createDatabase = properties.get( OgmProperties.CREATE_DATABASE );
		dataStore = CouchDBDatastore.newInstance(
				getDatabaseIdentifier(),
				createDatabase != null ? Boolean.valueOf( createDatabase.toString() ) : false
		);
	}

	private DatabaseIdentifier getDatabaseIdentifier() throws Exception {
		return new DatabaseIdentifier( getDatabaseHost(), getDatabasePort(), getDatabaseName(), getUserName(), getPassword() );
	}

	private void loadProperties() throws IOException {
		properties = new Properties();
		properties.putAll( environmentProperties() );
		properties.load( CouchDBDialectTest.class.getClassLoader().getResourceAsStream( "hibernate.properties" ) );
	}

	private String getPassword() {
		return properties.getProperty( OgmProperties.PASSWORD );
	}

	private String getUserName() {
		return properties.getProperty( OgmProperties.USERNAME );
	}

	private int getDatabasePort() {
		final String port = properties.getProperty( OgmProperties.PORT );
		if ( isValueProvided( port ) ) {
			return Integer.valueOf( port );
		}
		else {
			return DEFAULT_PORT;
		}
	}

	private String getDatabaseName() {
		return properties.getProperty( OgmProperties.DATABASE );
	}

	private String getDatabaseHost() {
		final String host = properties.getProperty( OgmProperties.HOST );
		if ( isValueProvided( host ) ) {
			return host;
		}
		else {
			return LOCALHOST;
		}
	}

	private EntityDocument createEntity() {
		return new EntityDocument( createEntityKey( "tableName", new String[] { "id", "name" }, new String[] { "1",
				"Andrea" } ) );
	}

	private EntityKey createEntityKey(String tableName, String[] columnNames, Object[] values) {
		return new EntityKey( new EntityKeyMetadata( tableName, columnNames ), values );
	}

	private DatabaseIdentifier getDatabaseIdentifierWithUnavailableHost() throws Exception {
		return new DatabaseIdentifier( "localhost", 1234, "no_existing", getUserName(), getPassword() );
	}

	private boolean isValueProvided(String property) {
		return property != null && !property.trim().equals( "" );
	}

}
