/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.hibernatecore.impl.OgmSession;
import org.hibernate.ogm.options.navigation.context.EntityContext;
import org.hibernate.ogm.options.navigation.context.GlobalContext;
import org.hibernate.ogm.options.navigation.context.PropertyContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;
import org.hibernate.ogm.service.impl.QueryParserService;
import org.hibernate.ogm.test.options.examples.EmbedExampleOption;
import org.hibernate.ogm.test.options.examples.ForceExampleOption;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.mapping.SampleOptionModel.SampleGlobalContext;
import org.hibernate.ogm.test.utils.OgmTestCase;
import org.hibernate.ogm.test.utils.TestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * An integration test that writes options using the fluent API and then reads them via the {@link OptionsService}.
 *
 * @author Gunnar Morling
 */
public class OptionIntegrationTest extends OgmTestCase {

	private OgmSession session;

	/**
	 * Not using the SF from the super class to reset it for each test method
	 */
	private SessionFactory sessions;

	@Before
	public void openOgmSession() {
		OgmConfiguration configuration = TestHelper.getDefaultTestConfiguration( getAnnotatedClasses() );
		configure( configuration );

		sessions = configuration.buildSessionFactory();
		session = (OgmSession) sessions.openSession();
	}

	@After
	public void closeSession() {
		session.close();
		sessions.close();
	}

	@Test
	public void testThatEntityOptionCanBeSetAndRetrieved() throws Exception {
		SampleGlobalContext configuration = session.configureDatastore( SampleNoSqlDatastore.class );
		configuration
			.entity( Refrigerator.class )
				.force( true );

		OptionsContainer refrigatorOptions = getOptionsContext().getEntityOptions( Refrigerator.class );

		ForceExampleOption forceOption = refrigatorOptions.getUnique( ForceExampleOption.class );
		assertThat( forceOption.isForced() ).isTrue();
	}

	@Test
	public void testThatEntityOptionsCanBeSetAndRetrievedOnMultipleTypes() throws Exception {
		SampleGlobalContext configuration = session.configureDatastore( SampleNoSqlDatastore.class );
		configuration
			.entity( Refrigerator.class )
				.force( true )
			.entity( Microwave.class )
				.name( "test" );

		OptionsContainer refrigatorOptions = getOptionsContext().getEntityOptions( Refrigerator.class );

		ForceExampleOption forceOption = refrigatorOptions.getUnique( ForceExampleOption.class );
		assertThat( forceOption.isForced() ).isTrue();

		OptionsContainer microwaveOptions = getOptionsContext().getEntityOptions( Microwave.class );

		NameExampleOption nameOption = microwaveOptions.getUnique( NameExampleOption.class );
		assertThat( nameOption.getName() ).isEqualTo( "test" );
	}

	@Test
	public void testThatPropertyOptionCanBeSetAndRetrieved() throws Exception {
		SampleGlobalContext configuration = session.configureDatastore( SampleNoSqlDatastore.class );
		configuration
			.entity( Refrigerator.class )
				.property( "temperature", ElementType.FIELD )
					.embed( "Embedded" );

		OptionsContainer temperatureOptions = getOptionsContext().getPropertyOptions( Refrigerator.class, "temperature" );

		EmbedExampleOption embeddedOption = temperatureOptions.getUnique( EmbedExampleOption.class );
		assertThat( embeddedOption.getEmbedded() ).isEqualTo( "Embedded" );
	}

	/**
	 * The requested type doesn't match with the type of global context created by the current datastore provider.
	 */
	@Test(expected = HibernateException.class)
	public void testThatWrongStoreTypeCausesException() {
		session.configureDatastore( AnotherDatastore.class );
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.getProperties().put( DatastoreProviderInitiator.DATASTORE_PROVIDER, SampleDatastoreProvider.class.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Refrigerator.class };
	}

	private OptionsServiceContext getOptionsContext() {
		return session.getSessionFactory()
				.getServiceRegistry()
				.getService( OptionsService.class )
				.context();
	}

	public static class Refrigerator {

		public int temperature;
	}

	public static class Microwave {

		public int power;
	}

	public static class SampleDatastoreProvider implements DatastoreProvider {

		@Override
		public Class<? extends GridDialect> getDefaultDialect() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
			throw new UnsupportedOperationException();
		}

		@Override
		public SampleGlobalContext getConfigurationBuilder(ConfigurationContext context) {
			return SampleOptionModel.createGlobalContext( context );
		}
	}

	public interface SampleNoSqlDatastore extends DatastoreConfiguration<SampleOptionModel.SampleGlobalContext> {
	}

	public interface AnotherDatastore extends DatastoreConfiguration<AnotherGlobalContext> {
	}

	public interface AnotherGlobalContext extends GlobalContext<AnotherGlobalContext, AnotherEntityContext> {
	}

	public interface AnotherEntityContext extends EntityContext<AnotherEntityContext, AnotherPropertyContext> {
	}

	public interface AnotherPropertyContext extends PropertyContext<AnotherEntityContext, AnotherPropertyContext> {
	}
}
