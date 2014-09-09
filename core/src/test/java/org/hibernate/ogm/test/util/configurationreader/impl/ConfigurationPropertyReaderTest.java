/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.util.configurationreader.impl;

import static org.fest.assertions.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.ogm.util.configurationreader.impl.DefaultClassPropertyReaderContext;
import org.hibernate.ogm.util.configurationreader.impl.Instantiator;
import org.hibernate.ogm.util.configurationreader.impl.Validators;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.ogm.util.configurationreader.spi.ShortNameResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for {@link ConfigurationPropertyReader}.
 *
 * @author Gunnar Morling
 */
public class ConfigurationPropertyReaderTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void shouldRetrievePropertyWithInstanceValue() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", new MyServiceImpl() );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class )
				.instantiate()
				.getValue();

		assertThat( value.getClass() ).isEqualTo( MyServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyWithClassValue() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", MyServiceImpl.class );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class )
				.instantiate()
				.getValue();

		assertThat( value.getClass() ).isEqualTo( MyServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyWithStringValue() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", MyServiceImpl.class.getName() );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class )
				.instantiate()
				.getValue();

		assertThat( value.getClass() ).isEqualTo( MyServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyWithDefaultImplementation() {
		Map<String, Object> properties = new HashMap<String, Object>();

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class )
				.instantiate()
				.withDefaultImplementation( MyOtherServiceImpl.class )
				.getValue();

		assertThat( value.getClass() ).isEqualTo( MyOtherServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyWithShortName() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", "other" );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class )
				.instantiate()
				.withShortNameResolver( new MyShortNameResolver() )
				.getValue();

		assertThat( value.getClass() ).isEqualTo( MyOtherServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyWithDefaultImplementationName() {
		Map<String, Object> properties = new HashMap<String, Object>();

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class )
				.instantiate()
				.withDefaultImplementation( MyServiceImpl.class.getName() )
				.getValue();

		assertThat( value.getClass() ).isEqualTo( MyServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyUsingCustomInstantiator() {
		Map<String, Object> properties = new HashMap<String, Object>();

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = ( (DefaultClassPropertyReaderContext<MyService>) reader.property( "service", MyService.class )
				.instantiate() )
				.withDefaultImplementation( MyYetAnotherServiceImpl.class )
				.withInstantiator( new MyInstantiator() )
				.getValue();

		assertThat( value.getClass() ).isEqualTo( MyYetAnotherServiceImpl.class );
	}

	@Test
	public void shouldRaiseExceptionDueToWrongInstanceType() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000046" );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", 42 );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );

		reader.property( "service", MyService.class )
				.instantiate()
				.getValue();
	}

	@Test
	public void shouldRaiseExceptionDueToWrongClassType() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000045" );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", Integer.class );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );

		reader.property( "service", MyService.class )
				.instantiate()
				.getValue();
	}

	@Test
	public void shouldRaiseExceptionDueToWrongClassTypeGivenAsString() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000045" );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", Integer.class.getName() );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );

		reader.property( "service", MyService.class )
				.instantiate()
				.getValue();
	}

	@Test
	public void shouldRetrieveStringProperty() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "foo", "bar" );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties );

		String value = reader.property( "foo", String.class ).getValue();
		assertThat( value ).isEqualTo( "bar" );
	}

	@Test
	public void shouldRetrieveIntProperty() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "foo", "123" );
		properties.put( "bar", 456 );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties );

		int value = reader.property( "foo", int.class ).getValue();
		assertThat( value ).isEqualTo( 123 );

		value = reader.property( "bar", int.class ).getValue();
		assertThat( value ).isEqualTo( 456 );
	}

	@Test
	public void shouldRetrieveBooleanProperty() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "foo", "true" );
		properties.put( "bar", true );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties );

		boolean value = reader.property( "foo", boolean.class ).getValue();
		assertThat( value ).isEqualTo( true );

		value = reader.property( "bar", boolean.class ).getValue();
		assertThat( value ).isEqualTo( true );
	}

	@Test
	public void shouldRetrieveEnumProperty() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "foo", "ANNOTATION_TYPE" );
		properties.put( "bar", ElementType.FIELD );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties );

		ElementType value = reader.property( "foo", ElementType.class ).getValue();
		assertThat( value ).isEqualTo( ElementType.ANNOTATION_TYPE );

		value = reader.property( "bar", ElementType.class ).getValue();
		assertThat( value ).isEqualTo( ElementType.FIELD );
	}

	@Test
	public void shouldRetrieveEnumPropertyWithDefaultValue() {
		Map<String, Object> properties = new HashMap<String, Object>();

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties );

		ElementType value = reader.property( "foo", ElementType.class )
				.withDefault( ElementType.ANNOTATION_TYPE )
				.getValue();
		assertThat( value ).isEqualTo( ElementType.ANNOTATION_TYPE );
	}

	@Test
	public void shouldRetrieveUrlPropertyGivenAsClassPathResource() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "configuration_resource", "configuration-test.properties" );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties );

		URL value = reader.property( "configuration_resource", URL.class ).getValue();
		assertThat( value ).isNotNull();

		Properties loadedProperties = loadPropertiesFromUrl( value );
		assertThat( loadedProperties.get( "hibernate.ogm.configuration.testproperty" ) ).isEqualTo( "foobar" );
	}

	@Test
	public void shouldRetrieveUrlPropertyGivenAsStringUrl() throws Exception {
		URL root = ConfigurationPropertyReaderTest.class.getClassLoader().getResource( "." );
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "configuration_resource", root.toExternalForm() + "/configuration-test.properties" );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties );

		URL value = reader.property( "configuration_resource", URL.class ).getValue();
		assertThat( value ).isNotNull();

		Properties loadedProperties = loadPropertiesFromUrl( value );
		assertThat( loadedProperties.get( "hibernate.ogm.configuration.testproperty" ) ).isEqualTo( "foobar" );
	}

	@Test
	public void shouldRetrieveUrlPropertyGivenAsFileSystemPath() throws Exception {
		File root = new File( ConfigurationPropertyReaderTest.class.getClassLoader().getResource( "." ).toURI() );
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "configuration_resource", root + File.separator + "configuration-test.properties" );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties );

		URL value = reader.property( "configuration_resource", URL.class ).getValue();
		assertThat( value ).isNotNull();

		Properties loadedProperties = loadPropertiesFromUrl( value );
		assertThat( loadedProperties.get( "hibernate.ogm.configuration.testproperty" ) ).isEqualTo( "foobar" );
	}

	@Test
	public void shouldRetrieveUrlPropertyGivenAsUrl() throws Exception {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "configuration_resource", new URL( "file://foobar/" ) );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties );

		URL value = reader.property( "configuration_resource", URL.class ).getValue();
		assertThat( value ).isEqualTo( new URL( "file://foobar/" ) );
	}

	@Test
	public void shouldRaiseExceptionDueToMissingRequiredProperty() {
		Map<String, Object> properties = new HashMap<String, Object>();

		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000052" );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties );

		reader.property( "foo", ElementType.class )
				.required()
				.getValue();
	}

	@Test
	public void shouldRaiseExceptionDueToInvalidPropertyValue() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "myPort", 98765 );

		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000049" );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties );

		reader.property( "myPort", int.class )
				.withValidator( Validators.PORT )
				.getValue();
	}

	private Properties loadPropertiesFromUrl(URL value) throws IOException {
		Properties properties = new Properties();
		InputStream stream = null;

		try {
			stream = value.openStream();
			properties.load( stream );
		}
		finally {
			if ( stream != null ) {
				stream.close();
			}
		}
		return properties;
	}

	private interface MyService {
	}

	public static class MyServiceImpl implements MyService {
	}

	public static class MyOtherServiceImpl implements MyService {
	}

	public static class MyYetAnotherServiceImpl implements MyService {

		public MyYetAnotherServiceImpl(String name) {
		}
	}

	private static class MyShortNameResolver implements ShortNameResolver {

		@Override
		public boolean isShortName(String name) {
			return "other".equals( name );
		}

		@Override
		public String resolve(String shortName) {
			return MyOtherServiceImpl.class.getName();
		}
	}

	private static class MyInstantiator implements Instantiator<MyService> {

		@Override
		public MyService newInstance(Class<? extends MyService> clazz) {
			return new MyYetAnotherServiceImpl( "foo" );
		}
	}
}
