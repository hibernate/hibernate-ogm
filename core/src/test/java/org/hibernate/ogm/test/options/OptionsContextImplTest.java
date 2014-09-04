/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.hibernate.ogm.util.impl.CollectionHelper.newHashMap;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.ogm.cfg.impl.InternalProperties;
import org.hibernate.ogm.options.navigation.impl.AppendableConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSources;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.examples.PermissionOption;
import org.hibernate.ogm.test.options.examples.annotations.NameExample;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link OptionsContextImpl}.
 *
 * @author Gunnar Morling
 */
public class OptionsContextImplTest {

	private AppendableConfigurationContext optionsServiceContext;
	private Map<String, Object> cfg;

	@Before
	public void setupContext() {
		optionsServiceContext = new AppendableConfigurationContext();
		cfg = newHashMap();
		cfg.put( InternalProperties.OGM_OPTION_CONTEXT, optionsServiceContext );
	}

	@Test
	public void shouldProvideUniqueOptionValueFromPropertyLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";
		optionsServiceContext.addPropertyOption( entityType, propertyName, new NameExampleOption(), "foobar" );

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foobar" );
	}

	@Test
	public void shouldProvideUniqueOptionValueFromEntityLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";

		optionsServiceContext.addEntityOption( entityType, new NameExampleOption(), "foobar" );

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foobar" );
	}

	@Test
	public void shouldProvideUniqueOptionValueFromGlobalLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";

		optionsServiceContext.addGlobalOption( new NameExampleOption(), "foobar" );

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foobar" );
	}

	@Test
	public void shouldProvideNonUniqueOptionValueFromPropertyLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";

		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "user" ), "read" );
		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "author" ), "read,write" );

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

		// then
		assertThat( context.get( PermissionOption.class, "user" ) ).isEqualTo( "read" );
		assertThat( context.get( PermissionOption.class, "author" ) ).isEqualTo( "read,write" );
	}

	@Test
	public void shouldProvideAllValuesForNonUniqueOptionFromPropertyLevel() {
		// given
		Class<?> entityType = Foo.class;
		String propertyName = "bar";

		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "user" ), "read" );
		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "author" ), "read,write" );

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

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

		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "user" ), "read" );
		optionsServiceContext.addPropertyOption( entityType, propertyName, new PermissionOption( "author" ), "read,write" );
		optionsServiceContext.addEntityOption( entityType, new PermissionOption( "admin" ), "read,write,delete" );

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

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

		optionsServiceContext.addEntityOption( entityType, new NameExampleOption(), "foobar" );
		optionsServiceContext.addPropertyOption( entityType, propertyName, new NameExampleOption(), "barfoo" );

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "barfoo" );
	}

	@Test
	public void shouldGiveOptionValueConfiguredViaApiPrecedenceOverAnnotation() {
		// given
		Class<?> entityType = Baz.class;
		String propertyName = "qux";

		optionsServiceContext.addPropertyOption( entityType, propertyName, new NameExampleOption(), "foo" );

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foo" );
	}

	@Test
	public void shouldGiveOptionValueConfiguredViaAnnotationOnPropertyLevelPrecedenceOverApiConfiguredValueOnEntityLevel() {
		// given
		Class<?> entityType = Baz.class;
		String propertyName = "qux";

		optionsServiceContext.addEntityOption( entityType, new NameExampleOption(), "foo" );

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "qux" );
	}

	@Test
	public void shouldProvideOptionValueConfiguredOnPropertyLevelForSuperClass() {
		// given
		Class<?> entityType = BazExt.class;
		String propertyName = "qux";

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "qux" );
	}

	@Test
	public void shouldGiveValueFromLocalPropertyLevelPrecedenceOverValueFromSuperTypePropertyLevel() {
		// given
		Class<?> entityType = BazExt.class;
		String propertyName = "qux";

		optionsServiceContext.addPropertyOption( entityType, propertyName, new NameExampleOption(), "foo" );

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foo" );
	}

	@Test
	public void shouldGiveValueFromSuperTypePropertyLevelPrecedenceOverValueFromLocalEntityLevel() {
		// given
		Class<?> entityType = BazExt.class;
		String propertyName = "qux";
		optionsServiceContext.addEntityOption( entityType, new NameExampleOption(), "foo" );

		// when
		OptionsContext context = OptionsContextImpl.forProperty( getSources(), entityType, propertyName );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "qux" );
	}

	@Test
	public void shouldProvideAccessToGlobalOptionSetViaProperty() {
		// given
		cfg.put( NameExampleOption.NAME_OPTION, "foobarqax" );

		// when
		OptionsContext context = OptionsContextImpl.forGlobal( getSources() );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foobarqax" );
	}

	@Test
	public void shouldGiveGlobalOptionSetViaApiPrecedenceOverValueSetViaProperty() {
		// given
		cfg.put( NameExampleOption.NAME_OPTION, "foobarqax" );
		optionsServiceContext.addGlobalOption( new NameExampleOption(), "foobar" );

		// when
		OptionsContext context = OptionsContextImpl.forGlobal( getSources() );

		// then
		assertThat( context.getUnique( NameExampleOption.class ) ).isEqualTo( "foobar" );
	}

	private List<OptionValueSource> getSources() {
		return OptionValueSources.getDefaultSources( new ConfigurationPropertyReader( cfg, new ClassLoaderServiceImpl() ) );
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
