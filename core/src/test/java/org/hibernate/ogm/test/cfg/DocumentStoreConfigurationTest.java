/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.cfg;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.impl.DocumentStoreConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for {@link DocumentStoreConfiguration}.
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 * @author Gunnar Morling
 */
public class DocumentStoreConfigurationTest {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private Map<String, String> configurationValues;
	private DocumentStoreConfiguration configuration;

	@Before
	public void setUp() {
		configurationValues = new HashMap<String, String>();
		configurationValues.put( OgmProperties.DATABASE, "foobar" );
	}

	@Test
	public void shouldReturnTheDefaultValueIfThePortKeyIsNotPresentAsAConfigurationValue() {
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getPort() ).isEqualTo( 1234 );
	}

	@Test
	public void shouldReturnTheDefaultValueIfThePortConfigurationValueIsTheEmptyString() {
		configurationValues.put( OgmProperties.PORT, "" );
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getPort() ).isEqualTo( 1234 );
	}

	@Test
	public void shouldReturnThePortConfigured() {
		final int configuredPortValue = 8080;
		configurationValues.put( OgmProperties.PORT, String.valueOf( configuredPortValue ) );
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getPort() ).isEqualTo( configuredPortValue );
	}

	@Test
	public void shouldReturnLocalhostIfTheHostNotPresentAsAConfigurationValue() {
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getHost() ).isEqualTo( "localhost" );
	}

	@Test
	public void shouldReturnLocalhostIfTheHostConfigurationValueIsTheEmptyString() {
		configurationValues.put( OgmProperties.HOST, " " );
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getHost() ).isEqualTo( "localhost" );
	}

	@Test
	public void shouldReturnTheHostConfiguredValue() {
		final String configuredHostValue = "192.168.2.2";
		configurationValues.put( OgmProperties.HOST, configuredHostValue );
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getHost() ).isEqualTo( configuredHostValue );
	}

	@Test
	public void shouldRaiseAnExceptionIfTheDatabaseNameConfigurationValueIsTheEmptyString() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000052" );

		configurationValues.put( OgmProperties.DATABASE, "" );
		new TestDocumentStoreConfiguration( configurationValues );
	}

	@Test
	public void shouldReturnTheDatabaseNameConfiguredValue() {
		final String configuredDatabaseName = "test";
		configurationValues.put( OgmProperties.DATABASE, configuredDatabaseName );
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getDatabaseName() ).isEqualTo( configuredDatabaseName );
	}

	@Test
	public void shouldReturnNullIfTheUsernameIsNotPresentAsAConfigurationValue() {
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getUsername() ).isNull();
	}

	@Test
	public void shouldReturnNullIfTheUsernameConfigurationValueIsTheEmptyString() {
		configurationValues.put( OgmProperties.USERNAME, "" );
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getUsername() ).isNull();
	}

	@Test
	public void shouldReturnTheUsernameConfiguredValue() {
		final String configuredUsername = "andrea";
		configurationValues.put( OgmProperties.USERNAME, configuredUsername );
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getUsername() ).isEqualTo( configuredUsername );
	}

	@Test
	public void shouldReturnNullIfThePasswordIsNotPresentAsAConfigurationValue() {
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getPassword() ).isNull();
	}

	@Test
	public void shouldReturnNullIfThePasswordConfigurationValueIsTheEmptyString() {
		configurationValues.put( OgmProperties.PASSWORD, "" );
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getPassword() ).isNull();
	}

	@Test
	public void shouldReturnThePasswordConfiguredValue() {
		final String configuredPassword = "pwd";
		configurationValues.put( OgmProperties.PASSWORD, configuredPassword );
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.getPassword() ).isEqualTo( configuredPassword );
	}

	@Test
	public void shouldReturnFalseIfTheDatabaseCreationValueIsNotPresentAsAConfigurationValue() {
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.isCreateDatabase() ).isFalse();
	}

	@Test
	public void shouldReturnFalseIfTheDatabaseCreationValueIsTheEmptyString() {
		configurationValues.put( OgmProperties.CREATE_DATABASE, "" );
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.isCreateDatabase() ).isFalse();
	}

	@Test
	public void shouldReturnTheDatabaseCreationConfiguredValue() {
		final String configuredValue = "true";
		configurationValues.put( OgmProperties.CREATE_DATABASE, configuredValue );
		configuration = new TestDocumentStoreConfiguration( configurationValues );

		assertThat( configuration.isCreateDatabase() ).isTrue();
	}

	private static class TestDocumentStoreConfiguration extends DocumentStoreConfiguration {

		public TestDocumentStoreConfiguration(Map<?, ?> configurationValues) {
			super( configurationValues, 1234 );
		}
	}
}
