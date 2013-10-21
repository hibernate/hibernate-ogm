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
package org.hibernate.ogm.test.options.mapping;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.ogm.options.generic.NamedQueryOption;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.OptionsContext;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.ogm.test.options.examples.EmbedExampleOption;
import org.hibernate.ogm.test.options.examples.ForceExampleOption;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.mapping.SampleOptionModel.SampleGlobalContext;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for putting/retrieving values into/from {@link OptionsContext}.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Gunnar Morling
 */
public class OptionsContextTest {

	private OptionsContext optionsContext;
	private SampleGlobalContext configuration;

	@Before
	public void setupContexts() {
		optionsContext = new OptionsContext();
		configuration = SampleOptionModel.createGlobalContext( new ConfigurationContext( optionsContext ) );
	}

	@Test
	public void contextShouldBeEmptyWhenCreated() throws Exception {
		assertThat( optionsContext.getGlobalOptions() ).isEmpty();
		assertThat( optionsContext.getEntityOptions( ContextExample.class ) ).isEmpty();
		assertThat( optionsContext.getPropertyOptions( ContextExample.class, "property" ) ).isEmpty();
	}

	@Test
	public void shouldBeAbleToAddGlobalOption() throws Exception {
		configuration.force( true );

		assertThat( optionsContext.getGlobalOptions() ).containsOnly( ForceExampleOption.TRUE );
	}

	@Test
	public void shouldBeAbleToAddNonUniqueGlobalOption() throws Exception {
		configuration
			.namedQuery( "foo", "from foo" )
			.namedQuery( "bar", "from bar" );

		Set<NamedQueryOption> queries = optionsContext.getGlobalOptions().get( NamedQueryOption.class );
		assertThat( queries ).containsOnly(
				new NamedQueryOption( "foo", "from foo" ),
				new NamedQueryOption( "bar", "from bar" )
		);
	}

	@Test
	public void shouldBeAbleToAddEntityOption() throws Exception {
		configuration
			.entity( ContextExample.class )
				.force( true );

		OptionsContainer optionsContainer = optionsContext.getEntityOptions( ContextExample.class );
		Iterator<Option<?>> iterator = optionsContainer.iterator();

		assertThat( iterator.next() ).as( "Unexpected option" ).isEqualTo( ForceExampleOption.TRUE );
		assertThat( iterator.hasNext() ).as( "Only one option should have been added per entity" ).isFalse();
	}

	@Test
	public void shouldBeAbleToAddSeveralEntityOptions() throws Exception {
		configuration
			.entity( Refrigerator.class )
				.force( true )
				.name( "test" );

		OptionsContainer refrigatorOptions = optionsContext.getEntityOptions( Refrigerator.class );

		ForceExampleOption forceOption = refrigatorOptions.getUnique( ForceExampleOption.class );
		assertThat( forceOption.isForced() ).isTrue();

		NameExampleOption nameOption = refrigatorOptions.getUnique( NameExampleOption.class );
		assertThat( nameOption.getName() ).isEqualTo( "test" );
	}

	@Test
	public void shouldBeAbleToAddPropertyOption() throws Exception {
		configuration
			.entity( ContextExample.class )
				.property( "property", ElementType.FIELD )
					.embed( "Foo" );

		OptionsContainer optionsContainer = optionsContext.getPropertyOptions( ContextExample.class, "property" );
		Iterator<Option<?>> iterator = optionsContainer.iterator();

		assertThat( iterator.next() ).as( "Unexpected option" ).isEqualTo( new EmbedExampleOption( "Foo" ) );
		assertThat( iterator.hasNext() ).as( "Only one options should have been added per property" ).isFalse();
	}

	@Test
	public void shouldBeAbleToRetrieveUniqueEntityOptionViaGet() throws Exception {
		configuration
			.entity( Refrigerator.class )
				.force( true );

		OptionsContainer refrigatorOptions = optionsContext.getEntityOptions( Refrigerator.class );

		Set<ForceExampleOption> forceOptions = refrigatorOptions.get( ForceExampleOption.class );
		assertThat( forceOptions ).hasSize( 1 );
		assertThat( forceOptions.iterator().next().isForced() ).isTrue();
	}

	@Test
	public void uniqueEntityOptionShouldHaveLastValueWhenSetSeveralTimes() throws Exception {
		configuration
			.entity( Refrigerator.class )
				.force( true )
				.force( false );

		OptionsContainer refrigatorOptions = optionsContext.getEntityOptions( Refrigerator.class );

		ForceExampleOption forceOption =  refrigatorOptions.getUnique( ForceExampleOption.class );
		assertThat( forceOption.isForced() ).isFalse();
	}

	private static class ContextExample {
	}

	public static class Refrigerator {

		public int temperature;
	}

	public static class Microwave {

		public int power;
	}
}
