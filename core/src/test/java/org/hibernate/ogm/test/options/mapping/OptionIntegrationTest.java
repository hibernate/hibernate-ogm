/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2014 Red Hat Inc. and/or its affiliates and other contributors
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
import org.hibernate.ogm.options.spi.OptionsContainer;
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
import org.hibernate.ogm.test.utils.OgmTestCase;
import org.hibernate.ogm.test.utils.TestHelper;
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

		OptionsContainer refrigatorOptions = getOptionsContext().getEntityOptions( Refrigerator.class );
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

		OptionsContainer refrigatorOptions = getOptionsContext().getEntityOptions( Refrigerator.class );
		assertThat( refrigatorOptions.getUnique( ForceExampleOption.class ) ).isTrue();

		OptionsContainer microwaveOptions = getOptionsContext().getEntityOptions( Microwave.class );
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

		OptionsContainer temperatureOptions = getOptionsContext().getPropertyOptions( Refrigerator.class, "temperature" );
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
		OptionsContainer refrigatorOptions = getOptionsContext().getEntityOptions( Refrigerator.class );
		assertThat( refrigatorOptions.getUnique( ForceExampleOption.class ) ).isTrue();

		OptionsContainer microwaveOptions = getOptionsContext().getEntityOptions( Microwave.class );
		assertThat( microwaveOptions.getUnique( NameExampleOption.class ) ).isEqualTo( "test" );

		OptionsContainer temperatureOptions = getOptionsContext().getPropertyOptions( Refrigerator.class, "temperature" );
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
