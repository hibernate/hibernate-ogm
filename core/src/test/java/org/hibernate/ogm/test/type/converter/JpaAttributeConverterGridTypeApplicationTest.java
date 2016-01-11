/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.type.converter;

import static org.fest.assertions.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.ogm.backendtck.type.converter.JpaAttributeConverterTest;
import org.hibernate.ogm.backendtck.type.converter.Printer;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.map.impl.MapDatastoreProvider;
import org.hibernate.ogm.datastore.map.impl.MapDialect;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestHelper;
import org.hibernate.type.BinaryType;
import org.hibernate.type.Type;
import org.junit.Test;

/**
 * Tests the JPA attribute converter logic in OGM. Uses a custom testing grid dialect, therefore it's not part of
 * the TCK. Amends {@link JpaAttributeConverterTest}.
 *
 * @author Gunnar Morling
 */
public class JpaAttributeConverterGridTypeApplicationTest extends OgmTestCase {

	/**
	 * String -> byte[] (which is tripled by the grid type).
	 */
	@Test
	public void convertedValueIsPersistedUsingCorrectGridType() throws Exception {
		Session session = openSession();
		session.getTransaction().begin();
		Printer printer = new Printer();
		printer.description = "somefoo";
		assertThat( printer.description ).isEqualTo( "somefoo" );
		session.persist( printer );
		session.getTransaction().commit();

		// Make sure the converter has actually been applied
		Map<String, Object> persistedTuple = TestHelper.extractEntityTuple(
				sessions,
				getPrinterEntityKey( printer.id )
		);
		byte[] persistedPrinterName = (byte[]) persistedTuple.get( "description" );
		assertThat( persistedPrinterName ).isEqualTo( "somefoosomefoosomefoo".getBytes( StandardCharsets.UTF_8 ) );
		session.clear();

		session.getTransaction().begin();
		printer = session.get( Printer.class, printer.id );
		assertThat( printer ).isNotNull();
		assertThat( printer.description ).isEqualTo( "somefoo" );
		session.delete( printer );
		session.getTransaction().commit();

		session.close();
	}

	private EntityKey getPrinterEntityKey(UUID id) {
		return new EntityKey(
				new DefaultEntityKeyMetadata( "Printer", new String[] { "id" } ),
				new Object[]{ id.toString() }
		);
	}

	@Override
	protected void configure(Map<String, Object> cfg) {
		cfg.put( OgmProperties.GRID_DIALECT, CustomMapDialect.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Printer.class
		};
	}

	public static class CustomMapDialect extends MapDialect {

		public CustomMapDialect(MapDatastoreProvider provider) {
			super( provider );
		}

		@Override
		public GridType overrideType(Type type) {
			if ( type == BinaryType.INSTANCE ) {
				return TriplingByteArrayGridType.INSTANCE;
			}
			else {
				return super.overrideType( type );
			}
		}
	}
}
