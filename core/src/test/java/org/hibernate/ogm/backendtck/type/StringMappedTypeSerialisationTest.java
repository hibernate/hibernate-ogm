/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.utils.OgmTestCase;

import static org.hibernate.ogm.utils.TestHelper.extractEntityTuple;
import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Nicolas Helleringer
 * @author Oliver Carr &lt;ocarr@redhat.com&gt;
 * @author Ajay Bhat
 * @author Hardy Ferentschik
 */
public class StringMappedTypeSerialisationTest extends OgmTestCase {
	private static final Random RANDOM = new Random();
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
		final Long userId = RANDOM.nextLong();
		b.setUserId( userId );
		final Integer stockCount = RANDOM.nextInt();
		b.setStockCount( stockCount );
		final Short urlPort = (short) 80;
		b.setUrlPort( urlPort );
		final Float visitRatio = (float) 10.4;
		b.setVisitRatio( visitRatio );
		final Character delimiter = '/';
		b.setDelimiter( delimiter );

		session.persist( b );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		b = (Bookmark) session.get( Bookmark.class, b.getId() );

		//Check directly in the cache the values stored
		EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata( "Bookmark", new String[] { "id" } );
		EntityKey key = new EntityKey( keyMetadata, new Object[] { b.getId() } );
		Map<String, Object> entity = extractEntityTuple( sessions, key );

		assertEquals( "Entity visits count incorrect", entity.get( "visitCount" ), "444" );
		assertEquals( "Entity serial number incorrect", entity.get( "serialNumber" ), serialNumber.toString() );
		assertEquals( "Entity URL incorrect", entity.get( "url" ), "http://www.hibernate.org/" );
		assertEquals( "Entity site weight incorrect", entity.get( "siteWeight" ), "21.77" );

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
