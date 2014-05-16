/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.mapping;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;

import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.options.navigation.EntityContext;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.PropertyContext;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;
import org.hibernate.ogm.test.options.examples.EmbedExampleOption;
import org.hibernate.ogm.test.options.examples.ForceExampleOption;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.mapping.model.Microwave;
import org.hibernate.ogm.test.options.mapping.model.Refrigerator;
import org.hibernate.ogm.test.options.mapping.model.SampleDatastoreProvider;
import org.hibernate.ogm.test.options.mapping.model.SampleNoSqlDatastore;
import org.hibernate.ogm.test.options.mapping.model.SampleOptionConfigurator;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.After;
import org.junit.Test;

/**
 * An integration test that writes options using the fluent API and then reads them via the {@link OptionsService}.
 *
 * @author Gunnar Morling
 */
public class OptionIntegrationTest extends OgmTestCase {

	private OgmSessionFactory sessions;

	@After
	public void closeSessionFactory() {
		sessions.close();
	}

	@Test
	public void testThatEntityOptionCanBeSetAndRetrieved() throws Exception {
		OgmConfiguration configuration = getConfiguration();
		configuration.configureOptionsFor( SampleNoSqlDatastore.class )
			.entity( Refrigerator.class )
				.force( true );

		setupSessionFactory( configuration );

		OptionsContext refrigatorOptions = getOptionsContext().getEntityOptions( Refrigerator.class );
		assertThat( refrigatorOptions.getUnique( ForceExampleOption.class ) ).isTrue();
	}

	@Test
	public void testThatEntityOptionsCanBeSetAndRetrievedOnMultipleTypes() throws Exception {
		OgmConfiguration configuration = getConfiguration();
		configuration.configureOptionsFor( SampleNoSqlDatastore.class )
			.entity( Refrigerator.class )
				.force( true )
			.entity( Microwave.class )
				.name( "test" );

		setupSessionFactory( configuration );

		OptionsContext refrigatorOptions = getOptionsContext().getEntityOptions( Refrigerator.class );
		assertThat( refrigatorOptions.getUnique( ForceExampleOption.class ) ).isTrue();

		OptionsContext microwaveOptions = getOptionsContext().getEntityOptions( Microwave.class );
		assertThat( microwaveOptions.getUnique( NameExampleOption.class ) ).isEqualTo( "test" );
	}

	@Test
	public void testThatPropertyOptionCanBeSetAndRetrieved() throws Exception {
		OgmConfiguration configuration = getConfiguration();
		configuration.configureOptionsFor( SampleNoSqlDatastore.class )
			.entity( Refrigerator.class )
				.property( "temperature", ElementType.FIELD )
					.embed( "Embedded" );

		setupSessionFactory( configuration );

		OptionsContext temperatureOptions = getOptionsContext().getPropertyOptions( Refrigerator.class, "temperature" );
		assertThat( temperatureOptions.getUnique( EmbedExampleOption.class ) ).isEqualTo( "Embedded" );
	}

	@Test
	public void testThatOptionsCanBeSetAndRetrievedUsingOptionConfiguratorInstance() throws Exception {
		OgmConfiguration configuration = getConfiguration();
		configuration.getProperties().put( OgmProperties.OPTION_CONFIGURATOR, new SampleOptionConfigurator() );
		setupSessionFactory( configuration );

		assertOptionsSetViaConfigurator();
	}

	@Test
	public void testThatOptionsCanBeSetAndRetrievedUsingOptionConfiguratorType() throws Exception {
		OgmConfiguration configuration = getConfiguration();
		configuration.getProperties().put( OgmProperties.OPTION_CONFIGURATOR, SampleOptionConfigurator.class );
		setupSessionFactory( configuration );

		assertOptionsSetViaConfigurator();
	}

	@Test
	public void testThatOptionsCanBeSetAndRetrievedUsingOptionConfiguratorTypeName() throws Exception {
		OgmConfiguration configuration = getConfiguration();
		configuration.getProperties().put( OgmProperties.OPTION_CONFIGURATOR, SampleOptionConfigurator.class.getName() );
		setupSessionFactory( configuration );

		assertOptionsSetViaConfigurator();
	}

	private void assertOptionsSetViaConfigurator() {
		OptionsContext refrigatorOptions = getOptionsContext().getEntityOptions( Refrigerator.class );
		assertThat( refrigatorOptions.getUnique( ForceExampleOption.class ) ).isTrue();

		OptionsContext microwaveOptions = getOptionsContext().getEntityOptions( Microwave.class );
		assertThat( microwaveOptions.getUnique( NameExampleOption.class ) ).isEqualTo( "test" );

		OptionsContext temperatureOptions = getOptionsContext().getPropertyOptions( Refrigerator.class, "temperature" );
		assertThat( temperatureOptions.getUnique( EmbedExampleOption.class ) ).isEqualTo( "Embedded" );
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.getProperties().put( OgmProperties.DATASTORE_PROVIDER, SampleDatastoreProvider.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Refrigerator.class };
	}

	private OptionsServiceContext getOptionsContext() {
		return sessions.getServiceRegistry()
				.getService( OptionsService.class )
				.context();
	}

	private void setupSessionFactory(OgmConfiguration ogmConfiguration) {
		sessions = ogmConfiguration.buildSessionFactory();
	}

	private OgmConfiguration getConfiguration() {
		OgmConfiguration configuration = TestHelper.getDefaultTestConfiguration( getAnnotatedClasses() );
		configuration.getProperties().put( OgmProperties.DATASTORE_PROVIDER, SampleDatastoreProvider.class );

		return configuration;
	}

	public interface AnotherGlobalContext extends GlobalContext<AnotherGlobalContext, AnotherEntityContext> {
	}

	public interface AnotherEntityContext extends EntityContext<AnotherEntityContext, AnotherPropertyContext> {
	}

	public interface AnotherPropertyContext extends PropertyContext<AnotherEntityContext, AnotherPropertyContext> {
	}
}
