/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type;

import static org.hibernate.ogm.utils.TestHelper.extractEntityTuple;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Nicolas Helleringer
 * @author Oliver Carr <ocarr@redhat.com>
 */
public class BuiltInTypeTest extends OgmTestCase {

	private static final Log log = LoggerFactory.make();

	private static final Random RANDOM = new Random();

	private static TimeZone originalTimeZone = null;

	@BeforeClass
	public static void setDefaultTimeZone() {
		originalTimeZone = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
	}

	@AfterClass
	public static void resetDefautlTimeZone() {
		TimeZone.setDefault( originalTimeZone );
	}

	@Test
	public void testTypesSupport() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();
		Bookmark b = new Bookmark();
		b.setId( "42" );
		b.setDescription( "Hibernate Site" );
		b.setUrl( new URL( "http://www.hibernate.org/" ) );
		BigDecimal weight = new BigDecimal( "21.77" );
		b.setSiteWeight( weight );
		BigInteger visitCount = new BigInteger( "444" );
		b.setVisitCount( visitCount );
		b.setFavourite( Boolean.TRUE );
		Byte displayMask = Byte.valueOf( (byte) '8' );
		b.setDisplayMask( displayMask );

		Date now = new Date( System.currentTimeMillis() );
		Calendar nowCalendar = Calendar.getInstance();
		nowCalendar.setTime( now );

		b.setCreationDate( now );
		b.setDestructionDate( now );
		b.setUpdateTime( now );
		b.setCreationCalendar( nowCalendar );
		b.setDestructionCalendar( nowCalendar );
		byte[] blob = new byte[5];
		blob[0] = '1';
		blob[1] = '2';
		blob[2] = '3';
		blob[3] = '4';
		blob[4] = '5';
		b.setBlob( blob );
		UUID serialNumber = UUID.randomUUID();
		b.setSerialNumber( serialNumber );
		final Long userId = RANDOM.nextLong();
		log.infof( "User ID created: $s", userId );
		b.setUserId( userId );
		final Integer stockCount = Integer.valueOf( RANDOM.nextInt() );
		b.setStockCount( stockCount );
		b.setType( BookmarkType.URL );

		session.persist( b );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		b = (Bookmark) session.get( Bookmark.class, b.getId() );
		assertEquals( "http://www.hibernate.org/", b.getUrl().toString() );
		assertEquals( weight, b.getSiteWeight() );
		assertEquals( visitCount, b.getVisitCount() );
		assertEquals( Boolean.TRUE, b.isFavourite() );
		assertEquals( displayMask, b.getDisplayMask() );
		assertEquals( "serial number incorrect", serialNumber, b.getSerialNumber() );
		assertEquals( "user id incorrect", userId, b.getUserId() );
		assertEquals( "stock count incorrect", stockCount, b.getStockCount() );

		//Date - DATE
		Calendar creationDate = Calendar.getInstance();
		creationDate.setTime( b.getCreationDate() );
		assertEquals( nowCalendar.get( Calendar.YEAR ), creationDate.get( Calendar.YEAR ) );
		assertEquals( nowCalendar.get( Calendar.MONTH ), creationDate.get( Calendar.MONTH ) );
		assertEquals( nowCalendar.get( Calendar.DAY_OF_MONTH ), creationDate.get( Calendar.DAY_OF_MONTH ) );

		//Date - TIME
		Calendar updateTime = Calendar.getInstance();
		updateTime.setTime( b.getUpdateTime() );
		assertEquals( nowCalendar.get( Calendar.HOUR_OF_DAY ), updateTime.get( Calendar.HOUR_OF_DAY ) );
		assertEquals( nowCalendar.get( Calendar.MINUTE ), updateTime.get( Calendar.MINUTE ) );
		assertEquals( nowCalendar.get( Calendar.SECOND ), updateTime.get( Calendar.SECOND ) );

		//Date - TIMESTAMP
		assertEquals( "Destruction date incorrect", now, b.getDestructionDate() );

		//Calendar - DATE
		assertEquals( "getCreationCalendar time zone incorrect", nowCalendar.getTimeZone().getRawOffset(), b.getCreationCalendar().getTimeZone().getRawOffset() );
		assertEquals( nowCalendar.get( Calendar.YEAR ), b.getCreationCalendar().get( Calendar.YEAR ) );
		assertEquals( nowCalendar.get( Calendar.MONTH ), b.getCreationCalendar().get( Calendar.MONTH ) );
		assertEquals( nowCalendar.get( Calendar.DAY_OF_MONTH ), b.getCreationCalendar().get( Calendar.DAY_OF_MONTH ) );

		//Calendar - TIMESTAMP
		assertEquals( "destructionCalendar time zone incorrect", nowCalendar.getTimeZone().getRawOffset(), b.getDestructionCalendar().getTimeZone().getRawOffset() );
		assertEquals( "destructionCalendar timestamp incorrect", nowCalendar.getTimeInMillis(), b.getDestructionCalendar().getTimeInMillis() );

		assertEquals( "Byte array incorrect length", blob.length, b.getBlob().length );
		assertEquals( blob[0], b.getBlob()[0] );
		assertEquals( '1', b.getBlob()[0] );
		assertEquals( '2', b.getBlob()[1] );
		assertEquals( '3', b.getBlob()[2] );
		assertEquals( '4', b.getBlob()[3] );
		assertEquals( '5', b.getBlob()[4] );

		assertEquals( BookmarkType.URL, b.getType() );

		session.delete( b );
		transaction.commit();

		session.close();
	}

	@Test
	public void testStringMappedTypeSerialisation() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Bookmark b = new Bookmark();
		b.setId( "42" );
		b.setUrl( new URL( "http://www.hibernate.org/" ) );
		BigDecimal weight = new BigDecimal( "21.77" );
		b.setSiteWeight( weight );
		BigInteger visitCount = new BigInteger( "444" );
		b.setVisitCount( visitCount );
		UUID serialNumber = UUID.randomUUID();
		b.setSerialNumber( serialNumber );
		final Long userId = RANDOM.nextLong();
		b.setUserId( userId );
		final Integer stockCount = Integer.valueOf( RANDOM.nextInt() );
		b.setStockCount( stockCount );

		session.persist( b );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		b = (Bookmark) session.get( Bookmark.class, b.getId() );

		//Check directly in the cache the values stored
		EntityKeyMetadata keyMetadata = new EntityKeyMetadata( "Bookmark", new String[]{ "id" } );
		EntityKey key = new EntityKey( keyMetadata, new Object[]{ "42" } );
		Map<String, Object> entity = extractEntityTuple( sessions, key );

		assertEquals( "Entity visits count incorrect", entity.get( "visits_count" ), "444" );
		assertEquals( "Entity serial number incorrect", entity.get( "serialNumber" ), serialNumber.toString() );
		assertEquals( "Entity URL incorrect", entity.get( "url" ), "http://www.hibernate.org/" );
		assertEquals( "Entity site weight incorrect", entity.get( "site_weight" ), "21.77" );

		session.delete( b );
		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{
				Bookmark.class
		};
	}
}
