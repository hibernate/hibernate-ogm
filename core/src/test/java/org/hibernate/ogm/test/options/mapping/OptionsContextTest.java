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

import java.util.Iterator;

import org.hibernate.ogm.options.navigation.impl.OptionsContext;
import org.hibernate.ogm.options.spi.Option;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.hibernate.ogm.test.options.examples.ForceExampleOption;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class OptionsContextTest {

	@Test
	public void contextShouldBeEmptyWhenCreated() throws Exception {
		OptionsContext context = new OptionsContext();

		assertThat( context.getGlobalOptions() ).isEmpty();
		assertThat( context.getEntityOptions( ContextExample.class ) ).isEmpty();
		assertThat( context.getPropertyOptions( ContextExample.class, "property" ) ).isEmpty();
	}

	@Test
	public void shouldBeAbleToAddGlobalOption() throws Exception {
		OptionsContext context = new OptionsContext();
		context.addGlobalOption( ForceExampleOption.TRUE );

		assertThat( context.getGlobalOptions() ).containsOnly( ForceExampleOption.TRUE );
	}

	@Test
	public void shouldBeAbleToAddEntityOption() throws Exception {
		OptionsContext context = new OptionsContext();
		context.configureEntity( ContextExample.class );
		context.addEntityOption( ForceExampleOption.TRUE );
		OptionsContainer optionsContainer = context.getEntityOptions( ContextExample.class );
		Iterator<Option<?>> iterator = optionsContainer.iterator();

		assertThat( iterator.next() ).as( "Unexpected option" ).isEqualTo( ForceExampleOption.TRUE );
		assertThat( iterator.hasNext() ).as( "Only one option should have been added per entity" ).isFalse();
	}

	@Test
	public void shouldBeAbleToAddPropertyOption() throws Exception {
		OptionsContext context = new OptionsContext();
		context.configureEntity( ContextExample.class );
		context.configureProperty( "property" );
		context.addPropertyOption( ForceExampleOption.TRUE );
		OptionsContainer optionsContainer = context.getPropertyOptions( ContextExample.class, "property" );
		Iterator<Option<?>> iterator = optionsContainer.iterator();

		assertThat( iterator.next() ).as( "Unexpected option" ).isEqualTo( ForceExampleOption.TRUE );
		assertThat( iterator.hasNext() ).as( "Only one options should have been added per property" ).isFalse();
	}

	private static class ContextExample {
	}
}
