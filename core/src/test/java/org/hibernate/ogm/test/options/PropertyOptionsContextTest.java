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
package org.hibernate.ogm.test.options;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

import org.hibernate.ogm.datastore.impl.PropertyOptionsContext;
import org.hibernate.ogm.datastore.spi.OptionsContext;
import org.hibernate.ogm.options.navigation.impl.WritableOptionsServiceContext;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.examples.PermissionOption;
import org.hibernate.ogm.test.options.examples.annotations.NameExample;
import org.junit.Test;

/**
 * Unit test for {@link PropertyOptionsContext}.
 *
 * @author Gunnar Morling
 */
public class PropertyOptionsContextTest {

	@Test
	public void shouldProvideUniqueOptionValueFromPropertyLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();
		optionsServiceContext.addPropertyOption( entityType, propertyName, new NameExampleOption(), "foobar" );

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foobar" );
	}

	@Test
	public void shouldProvideUniqueOptionValueFromEntityLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();
		optionsServiceContext.addEntityOption( entityType, new NameExampleOption(), "foobar" );

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foobar" );
	}

	@Test
	public void shouldProvideUniqueOptionValueFromGlobalLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();
		optionsServiceContext.addGlobalOption( new NameExampleOption(), "foobar" );

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foobar" );
	}

	@Test
	public void shouldProvideNonUniqueOptionValueFromPropertyLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();
		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "user" ), "read" );
		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "author" ), "read,write" );

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.get( PermissionOption.class, "user" ) ).isEqualTo( "read" );
		assertThat( context.get( PermissionOption.class, "author" ) ).isEqualTo( "read,write" );
	}

	@Test
	public void shouldProvideAllValuesForNonUniqueOptionFromPropertyLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();
		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "user" ), "read" );
		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "author" ), "read,write" );

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.getAll( PermissionOption.class ) ).
				hasSize( 2 )
				.includes(
						entry( "user", "read" ),
						entry( "author", "read,write" )
				);
	}

	@Test
	// TODO It might actually make sense to merge values from other levels or from super-types for non-unique options;
	// re-visit based on an actual use case
	public void shouldIgnoreValuesFromEntityWhenGettingAllValuesForNonUniqueOptionWhenValuesPresentOnPropertyLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();
		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "user" ), "read" );
		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "author" ), "read,write" );
		optionsServiceContext.addEntityOption( entityType, new PermissionOption( "admin" ), "read,write,delete" );

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.getAll( PermissionOption.class ) ).
				hasSize( 2 )
				.includes(
						entry( "user", "read" ),
						entry( "author", "read,write" )
				);
	}

	@Test
	public void shouldGiveOptionValueFromPropertyLevelPrecedenceOverEntityLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();
		optionsServiceContext.addEntityOption( entityType, new NameExampleOption(), "foobar" );
		optionsServiceContext.addPropertyOption( entityType, propertyName, new NameExampleOption(), "barfoo" );

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "barfoo" );
	}

	@Test
	public void shouldGiveOptionValueConfiguredViaApiPrecedenceOverAnnotation() {
		// given
		Class<?> entityType = Baz.class;
		String propertyName = "qux";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();
		optionsServiceContext.addPropertyOption( entityType, propertyName, new NameExampleOption(), "foo" );

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foo" );
	}

	@Test
	public void shouldGiveOptionValueConfiguredViaAnnotationOnPropertyLevelPrecedenceOverApiConfiguredValueOnEntityLevel() {
		// given
		Class<?> entityType = Baz.class;
		String propertyName = "qux";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();
		optionsServiceContext.addEntityOption( entityType, new NameExampleOption(), "foo" );

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "qux" );
	}

	@Test
	public void shouldProvideOptionValueConfiguredOnPropertyLevelForSuperClass() {
		// given
		Class<?> entityType = BazExt.class;
		String propertyName = "qux";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "qux" );
	}

	@Test
	public void shouldGiveValueFromLocalPropertyLevelPrecedenceOverValueFromSuperTypePropertyLevel() {
		// given
		Class<?> entityType = BazExt.class;
		String propertyName = "qux";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();
		optionsServiceContext.addPropertyOption( entityType, propertyName, new NameExampleOption(), "foo" );

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foo" );
	}

	@Test
	public void shouldGiveValueFromSuperTypePropertyLevelPrecedenceOverValueFromLocalEntityLevel() {
		// given
		Class<?> entityType = BazExt.class;
		String propertyName = "qux";
		WritableOptionsServiceContext optionsServiceContext = new WritableOptionsServiceContext();
		optionsServiceContext.addEntityOption( entityType, new NameExampleOption(), "foo" );

		// when
		OptionsContext context = new PropertyOptionsContext( optionsServiceContext, entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "qux" );
	}

	private static class Foo {

		@SuppressWarnings("unused")
		public String getBar() {
			return null;
		}
	}

	private static class Baz {

		@NameExample("qux")
		public String getQux() {
			return null;
		}
	}

	private static class BazExt extends Baz {

		@Override
		public String getQux() {
			return null;
		}
	}
}
