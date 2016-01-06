/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.custom;

import javax.persistence.AttributeConverter;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.ogm.utils.OgmTestCase;

import static org.fest.assertions.Assertions.assertThat;

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
		printer.name = new MyString( "SomeFoo" );
		assertThat( printer.name.toString() ).isEqualTo( "SomeFoo" );
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
	}
}
