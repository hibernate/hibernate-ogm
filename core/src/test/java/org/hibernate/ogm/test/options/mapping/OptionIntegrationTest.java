/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.mapping;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.engine.spi.OgmSessionFactoryImplementor;
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
import org.hibernate.ogm.test.options.mapping.model.SampleNoSqlDatastore;
import org.hibernate.ogm.test.options.mapping.model.SampleOptionConfigurator;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.After;
import org.junit.Test;

/**
 * An integration test that writes options using the fluent API and then reads them via the {@link OptionsService}.
 *
 * @author Gunnar Morling
 */
public class OptionIntegrationTest {

	private OgmSessionFactory sessions;

	@After
	public void closeSessionFactory() {
		sessions.close();
	}

	@Test
	public void testThatEntityOptionCanBeSetAndRetrieved() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, SampleNoSqlDatastore.class )
			.entity( Refrigerator.class )
				.force( true );

		setupSessionFactory( settings );

		OptionsContext refrigatorOptions = getOptionsContext().getEntityOptions( Refrigerator.class );
		assertThat( refrigatorOptions.getUnique( ForceExampleOption.class ) ).isTrue();
	}

	@Test
	public void testThatEntityOptionsCanBeSetAndRetrievedOnMultipleTypes() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, SampleNoSqlDatastore.class )
			.entity( Refrigerator.class )
				.force( true )
			.entity( Microwave.class )
				.name( "test" );

		setupSessionFactory( settings );

		OptionsContext refrigatorOptions = getOptionsContext().getEntityOptions( Refrigerator.class );
		assertThat( refrigatorOptions.getUnique( ForceExampleOption.class ) ).isTrue();

		OptionsContext microwaveOptions = getOptionsContext().getEntityOptions( Microwave.class );
		assertThat( microwaveOptions.getUnique( NameExampleOption.class ) ).isEqualTo( "test" );
	}

	@Test
	public void testThatPropertyOptionCanBeSetAndRetrieved() throws Exception {
		Map<String, Object> settings = new HashMap<String, Object>();

		TestHelper.configureOptionsFor( settings, SampleNoSqlDatastore.class )
			.entity( Refrigerator.class )
				.property( "temperature", ElementType.FIELD )
					.embed( "Embedded" );

		setupSessionFactory( settings );

		OptionsContext temperatureOptions = getOptionsContext().getPropertyOptions( Refrigerator.class, "temperature" );
		assertThat( temperatureOptions.getUnique( EmbedExampleOption.class ) ).isEqualTo( "Embedded" );
	}

	@Test
	public void testThatPropertyOptionCanBeSetViaOgmConfigurationAndRetrieved() throws Exception {
		OgmConfiguration cfg = new OgmConfiguration();
		cfg.configureOptionsFor( SampleNoSqlDatastore.class )
			.entity( Refrigerator.class )
				.property( "temperature", ElementType.FIELD )
					.embed( "Embedded" );

		sessions = cfg.buildSessionFactory();

		OptionsContext temperatureOptions = getOptionsContext().getPropertyOptions( Refrigerator.class, "temperature" );
		assertThat( temperatureOptions.getUnique( EmbedExampleOption.class ) ).isEqualTo( "Embedded" );
	}

	@Test
	public void testThatOptionsCanBeSetAndRetrievedUsingOptionConfiguratorInstance() throws Exception {
		Map<String, Object> settings = Collections.<String, Object>singletonMap( OgmProperties.OPTION_CONFIGURATOR, new SampleOptionConfigurator() );
		setupSessionFactory( settings );

		assertOptionsSetViaConfigurator();
	}

	@Test
	public void testThatOptionsCanBeSetAndRetrievedUsingOptionConfiguratorType() throws Exception {
		Map<String, Object> settings = Collections.<String, Object>singletonMap( OgmProperties.OPTION_CONFIGURATOR, SampleOptionConfigurator.class );
		setupSessionFactory( settings );

		assertOptionsSetViaConfigurator();
	}

	@Test
	public void testThatOptionsCanBeSetAndRetrievedUsingOptionConfiguratorTypeName() throws Exception {
		Map<String, Object> settings = Collections.<String, Object>singletonMap( OgmProperties.OPTION_CONFIGURATOR, SampleOptionConfigurator.class.getName() );
		setupSessionFactory( settings );

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

	private OptionsServiceContext getOptionsContext() {
		return ( (OgmSessionFactoryImplementor) sessions ).getServiceRegistry()
				.getService( OptionsService.class )
				.context();
	}

	private void setupSessionFactory(Map<String, Object> settings) {
		sessions = TestHelper.getDefaultTestSessionFactory( settings, Refrigerator.class );
	}

	public interface AnotherGlobalContext extends GlobalContext<AnotherGlobalContext, AnotherEntityContext> {
	}

	public interface AnotherEntityContext extends EntityContext<AnotherEntityContext, AnotherPropertyContext> {
	}

	public interface AnotherPropertyContext extends PropertyContext<AnotherEntityContext, AnotherPropertyContext> {
	}
}
