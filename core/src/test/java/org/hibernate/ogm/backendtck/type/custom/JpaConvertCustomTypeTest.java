/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.custom;

import static org.fest.assertions.Assertions.assertThat;

import java.net.URL;

import javax.persistence.AttributeConverter;

import org.hibernate.Session;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.type.descriptor.java.UrlTypeDescriptor;
import org.junit.Test;

/**
 * Test the JPA @Convert logic in OGM
 * TODO: should it be in the TCK, that's core code but it's nice to see it tested across all backends
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class JpaConvertCustomTypeTest extends OgmTestCase {

	@Test
	public void testJpaConvert() throws Exception {
		/*
		The Printer.name is a custom type.
		The converter converts it to a upper cased string when going to the datastore
		  This is tested as we expect an uppercased string when reading it in the converted
		The converter then converts back the string to a custom type using the lower cased string representation
		The test then makes sure lower case is what is read back at the object level
		 */
		Session session = openSession();
		session.getTransaction().begin();
		Printer printer = new Printer();
		printer.name = new MyString( "somefoo" );
		assertThat( printer.name.toString() ).isEqualTo( "somefoo" );
		session.persist( printer );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		printer = session.get( Printer.class, printer.id );
		assertThat( printer ).isNotNull();
		assertThat( printer.name.toString() ).isEqualTo( "somefoo" );
		session.delete( printer );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	public void testGridTypeForIntermediaryTypeIsUsed() throws Exception {
		Session session = openSession();
		session.getTransaction().begin();
		Printer printer = new Printer();
		printer.name = new MyString( "somefoo" );
		URL url = new URL( "http://example.com" );
		printer.url = url;
		session.persist( printer );
		session.getTransaction().commit();

		session.clear();

		session.getTransaction().begin();
		printer = session.get( Printer.class, printer.id );
		assertThat( printer ).isNotNull();
		assertThat( printer.url ).isEqualTo( url );
		session.delete( printer );
		session.getTransaction().commit();

		session.close();
	}

	@Test
	public void testGridTypeForIntermediaryTypeNotSupported() throws Exception {
		OgmConfiguration cfg = new OgmConfiguration();
		cfg.addAnnotatedClass( OtherPrinter.class );

		try {
			OgmSessionFactory sf = cfg.buildSessionFactory();
			sf.close();
			assertThat( true == false ).as( "We should fail as the AttributeConverter is not supported" );
		}
		catch (Exception e) {
			assertThat( e.getCause().getCause().getMessage() ).startsWith( "OGM000084" );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Printer.class
		};
	}

	public static class MyStringToUpperCaseAndBackConverter implements AttributeConverter<MyString,String> {

		@Override
		public String convertToDatabaseColumn(MyString attribute) {
			return attribute.toString().toUpperCase();
		}

		@Override
		public MyString convertToEntityAttribute(String dbData) {
			if ( ! dbData.equals( dbData.toUpperCase() ) ) {
				// test that we indeed stored as upper case
				throw new RuntimeException( "Error situation, convertToDatabaseColumn was not executed properly" );
			}
			return new MyString( dbData.toLowerCase() );
		}
	}

	/**
	 * @author Emmanuel Bernard emmanuel@hibernate.org
	 */
	public static class MyString {
		private final String string;

		public MyString(String string) {
			// note that the string is explicitly not lower case sanitized
			// we let the converter do this job to see if it is called
			this.string = string;
		}

		@Override
		public String toString() {
			return string;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( string == null ) ? 0 : string.hashCode() );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			MyString other = (MyString) obj;
			if ( string == null ) {
				if ( other.string != null ) {
					return false;
				}
			}
			else if ( !string.equals( other.string ) ) {
				return false;
			}
			return true;
		}
	}

	public static class URLToMyStringConverter implements AttributeConverter<URL, MyString> {

		@Override
		public MyString convertToDatabaseColumn(URL attribute) {
			if ( attribute == null ) {
				return null;
			}
			return new MyString( UrlTypeDescriptor.INSTANCE.toString( attribute ) );
		}

		@Override
		public URL convertToEntityAttribute(MyString dbData) {
			if ( dbData == null ) {
				return null;
			}
			return UrlTypeDescriptor.INSTANCE.fromString( dbData.toString() );
		}
	}

	public static class URLToURLConverter implements AttributeConverter<URL, URL> {

		@Override
		public URL convertToDatabaseColumn(URL attribute) {
			return attribute;
		}

		@Override
		public URL convertToEntityAttribute(URL dbData) {
			return dbData;
		}
	}
}
