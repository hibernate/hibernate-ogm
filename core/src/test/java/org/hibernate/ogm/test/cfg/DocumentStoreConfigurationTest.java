/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.cfg;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.spi.DocumentStoreConfiguration;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for {@link DocumentStoreConfiguration}.
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
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
			super( new ConfigurationPropertyReader( configurationValues ), 1234 );
		}
	}
}
