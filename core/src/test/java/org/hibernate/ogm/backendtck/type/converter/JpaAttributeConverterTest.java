/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.converter;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Test;

/**
 * Test the JPA @Convert logic in OGM
 * TODO: should it be in the TCK, that's core code but it's nice to see it tested across all backends
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class JpaAttributeConverterTest extends OgmTestCase {

	/**
	 * String -> String
	 */
	@Test
	public void jpaConverterIsApplied() throws Exception {
		Session session = openSession();
		session.getTransaction().begin();
		Printer printer = new Printer();
		printer.name = "somefoo";
		assertThat( printer.name ).isEqualTo( "somefoo" );
		session.persist( printer );
		session.getTransaction().commit();
		session.clear();

		session.getTransaction().begin();
		// Make sure the converter has actually been applied
		Map<String, Object> persistedTuple = TestHelper.extractEntityTuple(
				sessions,
				getPrinterEntityKey( printer.id )
		);
		String persistedPrinterName = (String) persistedTuple.get( "name" );
		assertThat( persistedPrinterName ).isEqualTo( "SOMEFOO" );
		session.getTransaction().commit();
		session.clear();

		session.getTransaction().begin();
		printer = session.get( Printer.class, printer.id );
		assertThat( printer ).isNotNull();
		assertThat( printer.name ).isEqualTo( "somefoo" );
		session.delete( printer );
		session.getTransaction().commit();

		session.close();
	}

	/**
	 * MyString -> String
	 */
	@Test
	public void jpaConverterIsAppliedToCustomType() throws Exception {
		Session session = openSession();
		session.getTransaction().begin();
		Printer printer = new Printer();
		printer.brand = new MyString( "printr inc." );
		assertThat( printer.brand.toString() ).isEqualTo( "printr inc." );
		session.persist( printer );
		session.getTransaction().commit();
		session.clear();

		session.getTransaction().begin();
		// Make sure the converter has actually been applied
		Map<String, Object> persistedTuple = TestHelper.extractEntityTuple(
				sessions,
				getPrinterEntityKey( printer.id )
		);
		String persistedPrinterName = (String) persistedTuple.get( "brand" );
		assertThat( persistedPrinterName ).isEqualTo( "PRINTR INC." );
		session.getTransaction().commit();
		session.clear();

		session.getTransaction().begin();
		printer = session.get( Printer.class, printer.id );
		assertThat( printer ).isNotNull();
		assertThat( printer.brand.toString() ).isEqualTo( "printr inc." );
		session.delete( printer );
		session.getTransaction().commit();

		session.close();
	}

	/**
	 * String -> MyString: Exception expected
	 */
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

	private EntityKey getPrinterEntityKey(UUID id) {
		return new EntityKey(
				new DefaultEntityKeyMetadata( "Printer", new String[] { "id" } ),
				new Object[]{ id.toString() }
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Printer.class };
	}
}
