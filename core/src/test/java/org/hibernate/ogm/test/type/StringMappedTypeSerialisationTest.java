/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.type;

import static org.hibernate.ogm.utils.TestHelper.extractEntityTuple;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.type.Bookmark;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Nicolas Helleringer
 * @author Oliver Carr &lt;ocarr@redhat.com&gt;
 * @author Ajay Bhat
 * @author Hardy Ferentschik
 */
public class StringMappedTypeSerialisationTest extends OgmTestCase {
	private Session session;

	@Before
	public void setup() {
		session = openSession();
	}

	@Test
	public void testStringMappedTypeSerialisation() throws Exception {
		Transaction transaction = session.beginTransaction();

		Bookmark b = new Bookmark();
		b.setUrl( new URL( "http://www.hibernate.org/" ) );
		BigDecimal weight = new BigDecimal( "21.77" );
		b.setSiteWeight( weight );
		BigInteger visitCount = new BigInteger( "444" );
		b.setVisitCount( visitCount );
		UUID serialNumber = UUID.randomUUID();
		b.setSerialNumber( serialNumber );

		session.persist( b );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		b = (Bookmark) session.get( Bookmark.class, b.getId() );

		//Check directly in the cache the values stored
		EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata( "Bookmark", new String[] { "id" } );
		EntityKey key = new EntityKey( keyMetadata, new Object[] { b.getId() } );
		Map<String, Object> entity = extractEntityTuple( session, key );

		assertEquals( "String-mapped BigInteger incorrect", entity.get( "visitCount" ), "444" );
		assertEquals( "String-mapped UUID incorrect", entity.get( "serialNumber" ), serialNumber.toString() );
		assertEquals( "String-mapped URL incorrect", entity.get( "url" ), "http://www.hibernate.org/" );
		assertEquals( "String-mapped BigDecimal incorrect", entity.get( "siteWeight" ), "21.77" );

		session.delete( b );
		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Bookmark.class
		};
	}
}
