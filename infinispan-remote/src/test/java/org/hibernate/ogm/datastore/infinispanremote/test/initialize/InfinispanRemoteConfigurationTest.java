/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.initialize;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties.CONFIGURATION_RESOURCE_NAME;
import static org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties.HOT_ROD_CLIENT_PREFIX;
import static org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties.SCHEMA_FILE_NAME;
import static org.infinispan.client.hotrod.impl.ConfigurationProperties.DEFAULT_EXECUTOR_FACTORY_QUEUE_SIZE;
import static org.infinispan.client.hotrod.impl.ConfigurationProperties.FORCE_RETURN_VALUES;
import static org.infinispan.client.hotrod.impl.ConfigurationProperties.MARSHALLER;
import static org.infinispan.client.hotrod.impl.ConfigurationProperties.TCP_NO_DELAY;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fest.assertions.Fail;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.configuration.impl.InfinispanRemoteConfiguration;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.OgmProtoStreamMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.utils.RemoteHotRodServerRule;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Test that the properties are set in the right way.
 * <p>
 * It is possible to override properties in the resource file programmatically.
 *
 * @author Davide D'Alto
 */
public class InfinispanRemoteConfigurationTest {

	@ClassRule
	public static final RemoteHotRodServerRule hotRodServer = new RemoteHotRodServerRule();

	/**
	 * Properties name to use in the hibernate configuration (when no resource file is used).
	 */
	private static final String OGM_FORCE_RETURN_VALUES_PROPERTY = HOT_ROD_CLIENT_PREFIX + "force_return_values";
	private static final String OGM_MARSHALLER_PROPERTY = HOT_ROD_CLIENT_PREFIX + "marshaller";
	private static final String OGM_EVICTION_MILLIS_PROPERTY = HOT_ROD_CLIENT_PREFIX + "timeBetweenEvictionRunsMillis";
	private static final String OGM_DEFAULT_EXECUTOR_FACTORY_QUEUE_SIZE_PROPERTY = HOT_ROD_CLIENT_PREFIX + "default_executor_factory.queue_size";

	private static final String EVICTION_MILLIS_PROPERTY = "timeBetweenEvictionRunsMillis";
	private static final String RESOURCE_NAME = "hotrod-client-testingconfiguration.properties";

	@Test
	public void shouldThrowExceptionWhenForceReturnValuesIsFalse() {
		assertExceptionIsThrown( OGM_FORCE_RETURN_VALUES_PROPERTY, "true", "false" );
	}

	@Test
	public void shouldThrowExceptionWhenAnotherMarshallerIsUsed() {
		assertExceptionIsThrown( OGM_MARSHALLER_PROPERTY, OgmProtoStreamMarshaller.class.getName(), ProtoStreamMarshaller.class.getName() );
	}

	private void assertExceptionIsThrown(String property, String expectedValue, String actualValue) {
		Map<String, Object> settings = new HashMap<>();
		settings.put( OgmProperties.DATASTORE_PROVIDER, GridDialectType.INFINISPAN_REMOTE.name() );
		settings.put( CONFIGURATION_RESOURCE_NAME, RESOURCE_NAME );
		settings.put( property, actualValue );

		try {
			extractClientConfiguration( settings );
			Fail.fail( "There should be an exception because of the wrong configuration value" );
		}
		catch (org.hibernate.service.spi.ServiceException ex) {
			assertThat( ex.getCause() ).isInstanceOf( HibernateException.class );
			assertThat( ex.getCause().getMessage() ).isEqualTo( "OGM001715: Property <" + property.replace( HOT_ROD_CLIENT_PREFIX, "infinispan.client.hotrod." ) + "> has to be set to <" + expectedValue + "> but it's set to <" + actualValue + ">" );
		}
	}

	@Test
	@TestForIssue( jiraKey = "OGM-1347" )
	public void shouldUseDefaultProtoNameIfNoneIsSet() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( OgmProperties.DATASTORE_PROVIDER, GridDialectType.INFINISPAN_REMOTE.name() );
		settings.put( CONFIGURATION_RESOURCE_NAME, RESOURCE_NAME );

		InfinispanRemoteConfiguration configuration = extractConfiguration( settings );
		assertThat( configuration.getSchemaFileName() ).isEqualTo( InfinispanRemoteProperties.DEFAULT_SCHEMA_FILE_NAME );
	}

	@Test
	@TestForIssue( jiraKey = "OGM-1347" )
	public void shouldThrowExceptionWhenProtoFileNameIsInvalid() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( OgmProperties.DATASTORE_PROVIDER, GridDialectType.INFINISPAN_REMOTE.name() );
		settings.put( CONFIGURATION_RESOURCE_NAME, RESOURCE_NAME );
		settings.put( SCHEMA_FILE_NAME, "invalidproto.wrongextention" );

		try {
			extractClientConfiguration( settings );
			Fail.fail( "There should be an exception because of the wrong configuration value" );
		}
		catch (org.hibernate.service.spi.ServiceException ex) {
			assertThat( ex.getCause() ).isInstanceOf( HibernateException.class );
			assertThat( ex.getCause().getMessage() ).isEqualTo( "OGM001717: Invalid Proto file name <invalidproto.wrongextention>. Proto file name should match the pattern: *.proto" );
		}
	}

	@Test
	@TestForIssue( jiraKey = "OGM-1347" )
	public void shouldBePossibleToChangeProtoFileName() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( OgmProperties.DATASTORE_PROVIDER, GridDialectType.INFINISPAN_REMOTE.name() );
		settings.put( CONFIGURATION_RESOURCE_NAME, RESOURCE_NAME );
		settings.put( SCHEMA_FILE_NAME, "rightName.proto" );

		InfinispanRemoteConfiguration configuration = extractConfiguration( settings );
		assertThat( configuration.getSchemaFileName() ).isEqualTo( "rightName.proto" );
	}

	@Test
	public void shouldSetDefaultValues() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( OgmProperties.DATASTORE_PROVIDER, GridDialectType.INFINISPAN_REMOTE.name() );

		Properties clientProperties = extractClientConfiguration( settings );

		assertThat( clientProperties ).isNotEmpty();
		assertThat( clientProperties.getProperty( MARSHALLER ) ).isEqualTo( OgmProtoStreamMarshaller.class.getName() );
		assertThat( clientProperties.getProperty( FORCE_RETURN_VALUES ) ).isEqualTo( "true" );
	}

	@Test
	public void shouldNormalizeTheValueBeforeValidation() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( OgmProperties.DATASTORE_PROVIDER, GridDialectType.INFINISPAN_REMOTE.name() );
		settings.put( CONFIGURATION_RESOURCE_NAME, RESOURCE_NAME );
		settings.put( OGM_FORCE_RETURN_VALUES_PROPERTY, "  TRUE " );

		Properties clientProperties = extractClientConfiguration( settings );

		assertThat( clientProperties ).isNotEmpty();
		assertThat( clientProperties.getProperty( FORCE_RETURN_VALUES ) ).isEqualTo( "TRUE" );
	}

	@Test
	public void shouldBeAbleToReadTheResourceFile() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( OgmProperties.DATASTORE_PROVIDER, GridDialectType.INFINISPAN_REMOTE.name() );
		settings.put( CONFIGURATION_RESOURCE_NAME, RESOURCE_NAME );

		Properties clientProperties = extractClientConfiguration( settings );

		assertThat( clientProperties ).isNotEmpty();
		assertThat( clientProperties.getProperty( TCP_NO_DELAY ) ).isEqualTo( "true" );
		assertThat( clientProperties.getProperty( DEFAULT_EXECUTOR_FACTORY_QUEUE_SIZE ) ).isEqualTo( "100" );
		assertThat( clientProperties.getProperty( EVICTION_MILLIS_PROPERTY ) ).isEqualTo( "120000" );
	}

	@Test
	public void shouldOverrideConfigurationInResourceFile() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( OgmProperties.DATASTORE_PROVIDER, GridDialectType.INFINISPAN_REMOTE.name() );
		settings.put( CONFIGURATION_RESOURCE_NAME, RESOURCE_NAME );

		// Set the same property in the resource file
		settings.put( OGM_DEFAULT_EXECUTOR_FACTORY_QUEUE_SIZE_PROPERTY, "99" );
		settings.put( OGM_EVICTION_MILLIS_PROPERTY, "129999" );

		Properties clientProperties = extractClientConfiguration( settings );

		assertThat( clientProperties ).isNotEmpty();

		// From the resource file (didn't replace this)
		assertThat( clientProperties.getProperty( TCP_NO_DELAY ) ).isEqualTo( "true" );

		// The one I set manually
		assertThat( clientProperties.getProperty( DEFAULT_EXECUTOR_FACTORY_QUEUE_SIZE ) ).isEqualTo( "99" );
		assertThat( clientProperties.getProperty( EVICTION_MILLIS_PROPERTY ) ).isEqualTo( "129999" );
	}

	private Properties extractClientConfiguration(Map<String, Object> settings) {
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory( settings ) ) {
			ServiceRegistryImplementor serviceRegistry = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry();
			InfinispanRemoteConfiguration conf = new InfinispanRemoteConfiguration();
			conf.initConfiguration( settings, serviceRegistry );
			return conf.getClientProperties();
		}
	}

	private InfinispanRemoteConfiguration extractConfiguration(Map<String, Object> settings) {
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory( settings ) ) {
			ServiceRegistryImplementor serviceRegistry = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry();
			InfinispanRemoteConfiguration conf = new InfinispanRemoteConfiguration();
			conf.initConfiguration( settings, serviceRegistry );
			return conf;
		}
	}

}
