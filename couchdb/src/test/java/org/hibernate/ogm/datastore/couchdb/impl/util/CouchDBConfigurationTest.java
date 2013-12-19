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
package org.hibernate.ogm.datastore.couchdb.impl.util;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.datastore.couchdb.CouchDB;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero <dreborier@gmail.com>
 */
public class CouchDBConfigurationTest {

	private Map<String, String> configurationValues;
	private CouchDBConfiguration configuration;

	@Before
	public void setUp() {
		configurationValues = new HashMap<String, String>();
		configuration = new CouchDBConfiguration();
	}

	@Test
	public void shouldReturnTheDefaultValueIfThePortKeyIsNotPresentAsAConfigurationValue() {
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getDatabasePort(), is( Integer.valueOf( CouchDBConfiguration.DEFAULT_COUCHDB_PORT ) ) );
	}

	@Test
	public void shouldReturnTheDefaultValueIfThePortConfigurationValueIsTheEmptyString() {
		configurationValues.put( CouchDB.PORT, "" );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getDatabasePort(), is( Integer.valueOf( CouchDBConfiguration.DEFAULT_COUCHDB_PORT ) ) );
	}

	@Test
	public void shouldReturnThePortConfigured() {
		final int configuredPortValue = 8080;
		configurationValues.put( CouchDB.PORT, String.valueOf( configuredPortValue ) );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getDatabasePort(), is( configuredPortValue ) );
	}

	@Test
	public void shouldReturnLocalhostIfTheHostNotPresentAsAConfigurationValue() {
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getDatabaseHost(), is( "localhost" ) );
	}

	@Test
	public void shouldReturnLocalhostIfTheHostConfigurationValueIsTheEmptyString() {
		configurationValues.put( CouchDB.HOST, " " );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getDatabaseHost(), is( "localhost" ) );
	}

	@Test
	public void shouldReturnTheHostConfiguredValue() {
		final String configuredHostValue = "192.168.2.2";
		configurationValues.put( CouchDB.HOST, configuredHostValue );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getDatabaseHost(), is( configuredHostValue ) );
	}

	@Test
	public void shouldReturnNullIfTheDatabaseNameIsNotPresentAsAConfigurationValue() {
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getDatabaseName(), nullValue() );
	}

	@Test
	public void shouldReturnNullIfTheDatabaseNameConfigurationValueIsTheEmptyString() {
		configurationValues.put( CouchDB.DATABASE, "" );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getDatabaseName(), nullValue() );
	}

	@Test
	public void shouldReturnTheDatabaseNameConfiguredValue() {
		final String configuredDatabaseName = "test";
		configurationValues.put( CouchDB.DATABASE, configuredDatabaseName );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getDatabaseName(), is( configuredDatabaseName ) );
	}

	@Test
	public void shouldReturnNullIfTheUsernameIsNotPresentAsAConfigurationValue() {
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getUsername(), nullValue() );
	}

	@Test
	public void shouldReturnNullIfTheUsernameConfigurationValueIsTheEmptyString() {
		configurationValues.put( CouchDB.USERNAME, "" );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getUsername(), nullValue() );
	}

	@Test
	public void shouldReturnTheUserameConfiguredValue() {
		final String configuredUsername = "andrea";
		configurationValues.put( CouchDB.USERNAME, configuredUsername );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getUsername(), is( configuredUsername ) );
	}

	@Test
	public void shouldReturnNullIfThePasswordIsNotPresentAsAConfigurationValue() {
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getPassword(), nullValue() );
	}

	@Test
	public void shouldReturnNullIfThePasswordConfigurationValueIsTheEmptyString() {
		configurationValues.put( CouchDB.PASSWORD, "" );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getPassword(), nullValue() );
	}

	@Test
	public void shouldReturnThePasswordConfiguredValue() {
		final String configuredPassword = "pwd";
		configurationValues.put( CouchDB.PASSWORD, configuredPassword );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.getPassword(), is( configuredPassword ) );
	}

	@Test
	public void shouldReturnFalseIfTheDatabaseCreationValueIsNotPresentAsAConfigurationValue() {
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.isDatabaseToBeCreated(), is( false ) );
	}

	@Test
	public void shouldReturnFalseIfTheDatabaseCreationValueIsTheEmptyString() {
		configurationValues.put( CouchDB.CREATE_DATABASE, "" );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.isDatabaseToBeCreated(), is( false ) );
	}

	@Test
	public void shouldReturnTheDatabaseCreationConfiguredValue() {
		final String configuredValue = "true";
		configurationValues.put( CouchDB.CREATE_DATABASE, configuredValue );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.isDatabaseToBeCreated(), is( Boolean.valueOf( configuredValue ) ) );
	}

	@Test
	public void shouldIsDatabaseNameConfiguredReturnTrueIfTheDatabaseNameIsPresentInTheConfiguredValues() {
		final String configuredDatabaseName = "test";
		configurationValues.put( CouchDB.DATABASE, configuredDatabaseName );
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.isDatabaseNameConfigured(), is( true ) );
	}

	@Test
	public void shouldIsDatabaseNameConfiguredReturnFalseIfTheDatabaseNameIsNotPresentInTheConfiguredValues() {
		configuration.setConfigurationValues( configurationValues );

		assertThat( configuration.isDatabaseNameConfigured(), is( false ) );
	}

}
