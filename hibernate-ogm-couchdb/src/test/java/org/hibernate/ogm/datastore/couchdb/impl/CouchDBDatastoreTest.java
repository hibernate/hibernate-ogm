/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.couchdb.impl;

import org.hibernate.HibernateException;
import org.hibernate.ogm.dialect.couchdb.CouchDBDialectTest;
import org.hibernate.ogm.dialect.couchdb.Environment;
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBDocument;
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBEntity;
import org.hibernate.ogm.dialect.couchdb.util.DataBaseURL;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.OptimisticLockException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
public class CouchDBDatastoreTest {

	public static final int DEFAULT_PORT = 5984;
	public static final String LOCALHOST = "localhost";
	private CouchDBDatastore dataStore;
	private Properties properties;
	private boolean dataBaseDropped;

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
		CouchDBDocument createdDocument = dataStore.saveDocument( createEntity() );
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
	public void testCreatingInstanceWithWrongUrlUpdatingADocumentUsingAWrongDatabaseUrlConfiguration() throws Exception {
		dataStore = null;
		CouchDBDatastore.newInstance( getWrongDatabaseURL(), getUserName(), getPassword(), true );
	}

	@Test
	public void testDeleteADocument() {
		CouchDBDocument createdDocument = dataStore.saveDocument( createEntity() );
		String createdDocumentId = createdDocument.getId();
		dataStore.deleteDocument( createdDocument );

		CouchDBEntity entity = dataStore.getEntity( createdDocumentId );

		assertThat( entity, nullValue() );
	}

	@Test(expected = OptimisticLockException.class)
	public void testDeleteADocumentWithWrongRevision() {
		CouchDBDocument createdDocument = dataStore.saveDocument( createEntity() );
		final String revisionBeforeUpdate = createdDocument.getRevision();

		// saving the document will change its revision value
		dataStore.saveDocument( createdDocument );
		createdDocument.setRevision( revisionBeforeUpdate );

		dataStore.deleteDocument( createdDocument );
	}

	@Test(expected = HibernateException.class)
	public void testDeleteADocumentNotSavedOnTheDatabase() {
		CouchDBDocument document = createEntity();
		dataStore.deleteDocument( document );
	}

	@Test
	public void testGetEntity() {
		CouchDBDocument createdDocument = dataStore.saveDocument( createEntity() );
		CouchDBEntity entity = dataStore.getEntity( createdDocument.getId() );
		assertThat( entity, notNullValue() );
	}

	@Test
	public void testGetEntityWithWoringIdReturnNullValue() {
		CouchDBDocument createdDocument = dataStore.saveDocument( createEntity() );
		CouchDBEntity entity = dataStore.getEntity( createdDocument.getId() + "_1" );
		assertThat( entity, nullValue() );
	}

	private void setUpDatastore() throws IOException {
		dataBaseDropped = false;
		loadProperties();
		dataStore = CouchDBDatastore.newInstance( getDatabaseURL(), getUserName(), getPassword(), true );
	}

	private DataBaseURL getDatabaseURL() throws MalformedURLException {
		return new DataBaseURL( getDatabaseHost(), getDatabasePort(), getDatabaseName() );
	}

	private void loadProperties() throws IOException {
		properties = new Properties();
		properties.load( CouchDBDialectTest.class.getClassLoader().getResourceAsStream( "hibernate.properties" ) );
	}

	private String getPassword() {
		return properties.getProperty( Environment.COUCHDB_PASSWORD );
	}

	private String getUserName() {
		return properties.getProperty( Environment.COUCHDB_USERNAME );
	}

	private int getDatabasePort() {
		final String port = properties.getProperty( Environment.COUCHDB_PORT );
		if ( isValueProvided( port ) ) {
			return Integer.valueOf( port );
		}
		else {
			return DEFAULT_PORT;
		}
	}

	private String getDatabaseName() {
		return properties.getProperty( Environment.COUCHDB_DATABASE );
	}

	private String getDatabaseHost() {
		final String host = properties.getProperty( Environment.COUCHDB_HOST );
		if ( isValueProvided( host ) ) {
			return host;
		}
		else {
			return LOCALHOST;
		}
	}

	private CouchDBEntity createEntity() {
		return new CouchDBEntity( createEntityKey( "tableName", new String[] { "id", "name" }, new String[] { "1",
				"Andrea" } ) );
	}

	private EntityKey createEntityKey(String tableName, String[] columnNames, Object[] values) {
		return new EntityKey( new EntityKeyMetadata( tableName, columnNames ), values );
	}

	private DataBaseURL getWrongDatabaseURL() throws MalformedURLException {
		return new DataBaseURL( "localhost", 1234, "no_existing" );
	}

	private boolean isValueProvided(String property) {
		return property != null && !property.trim().equals( "" );
	}

}
