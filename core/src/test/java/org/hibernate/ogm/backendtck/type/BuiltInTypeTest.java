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
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.backendtck.type.Bookmark.Classifier;
import org.hibernate.ogm.utils.OgmTestCase;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Nicolas Helleringer
 * @author Oliver Carr &lt;ocarr@redhat.com&gt;
 * @author Ajay Bhat
 * @author Hardy Ferentschik
 */
public class BuiltInTypeTest extends OgmTestCase {
	private static final Random RANDOM = new Random();
	private static TimeZone originalTimeZone = null;

	private Calendar calendar;
	private Session session;
	private Bookmark bookmark;

	@BeforeClass
	public static void setDefaultTimeZone() {
		originalTimeZone = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
	}

	@Before
	public void setup() {
		session = openSession();
		calendar = Calendar.getInstance();
		bookmark = new Bookmark();
	}

	@AfterClass
	public static void resetDefaultTimeZone() {
		TimeZone.setDefault( originalTimeZone );
	}

	// basic types
	@Test
	public void testStringSupport() throws Exception {
		bookmark.setDescription( "Hibernate Site" );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "String value does not match", bookmark.getDescription(), loadedBookmark.getDescription() );
	}

	@Test
	public void testCharacterSupport() throws Exception {
		bookmark.setDelimiter( '/' );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "Character value does not match", bookmark.getDelimiter(), loadedBookmark.getDelimiter() );
	}

	@Test
	public void testIntegerSupport() throws Exception {
		bookmark.setStockCount( RANDOM.nextInt() );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "Integer value does not match", bookmark.getStockCount(), loadedBookmark.getStockCount() );
	}

	@Test
	public void testShortSupport() throws Exception {
		bookmark.setUrlPort( (short) 80 );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "Short value does not match", bookmark.getUrlPort(), loadedBookmark.getUrlPort() );
	}

	@Test
	public void testLongSupport() throws Exception {
		bookmark.setUserId( RANDOM.nextLong() );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "Long value does not match", bookmark.getUserId(), loadedBookmark.getUserId() );
	}

	@Test
	public void testFloatSupport() throws Exception {
		bookmark.setVisitRatio( (float) 10.4 );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "Long value does not match", bookmark.getVisitRatio(), loadedBookmark.getVisitRatio() );
	}

	@Test
	public void testDoubleSupport() throws Exception {
		bookmark.setTaxPercentage( 12.34d );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "Long value does not match", bookmark.getTaxPercentage(), loadedBookmark.getTaxPercentage() );
	}

	@Test
	public void testBooleanSupport() throws Exception {
		bookmark.setFavourite( Boolean.TRUE );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "Boolean value does not match", bookmark.getFavourite(), loadedBookmark.getFavourite() );
	}

	@Test
	public void testByteSupport() throws Exception {
		bookmark.setDisplayMask( (byte) '8' );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "Byte value does not match", bookmark.getDisplayMask(), loadedBookmark.getDisplayMask() );
	}

	// byte arrays
	@Test
	public void testByteArrayAsLobSupport() throws Exception {
		byte[] testData = new byte[200];
		new Random().nextBytes( testData );
		bookmark.setLob( testData );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertArrayEquals( "Original and loaded data do not match!", testData, loadedBookmark.getLob() );
	}

	@Test
	public void testByteArraySupport() throws Exception {
		byte[] testData = new byte[200];
		new Random().nextBytes( testData );
		bookmark.setData( testData );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertArrayEquals( "Original and loaded data do not match!", testData, loadedBookmark.getData() );
	}

	// enum types
	@Test
	public void testEnumTypeMappedAsStringSupport() throws Exception {
		bookmark.setClassifier( Classifier.HOME );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals(
				"String mapped enum value does not match", bookmark.getClassifier(), loadedBookmark.getClassifier()
		);
	}

	@Test
	public void testEnumTypeMappedAsOrdinalSupport() throws Exception {
		bookmark.setClassifierAsOrdinal( Classifier.WORK );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals(
				"Ordinal mapped enum value does not match", bookmark.getClassifierAsOrdinal(),
				loadedBookmark.getClassifierAsOrdinal()
		);
	}

	// Date/time types
	@Test
	public void testDatePersistedAsTemporalTypeDateSupport() throws Exception {
		Date creationDate = new Date();
		bookmark.setCreationDate( creationDate );

		// TemporalType#Date only deals with year/month/day
		calendar.setTime( creationDate );
		int expectedYear = calendar.get( Calendar.YEAR );
		int expectedMonth = calendar.get( Calendar.MONTH );
		int expectedDay = calendar.get( Calendar.DAY_OF_MONTH );
		int expectedTimeZoneOffset = calendar.getTimeZone().getRawOffset();

		Bookmark loadedBookmark = saveAndGet( bookmark );
		calendar.setTime( loadedBookmark.getCreationDate() );
		int actualYear = calendar.get( Calendar.YEAR );
		int actualMonth = calendar.get( Calendar.MONTH );
		int actualDay = calendar.get( Calendar.DAY_OF_MONTH );
		int actualTimeZoneOffset = calendar.getTimeZone().getRawOffset();

		assertEquals( "Year value does not match", expectedYear, actualYear );
		assertEquals( "Month value does not match", expectedMonth, actualMonth );
		assertEquals( "Day value does not match", expectedDay, actualDay );
		assertEquals( "Time zones doe not match", expectedTimeZoneOffset, actualTimeZoneOffset );
	}

	@Test
	public void testDatePersistedAsTemporalTypeTimeSupport() throws Exception {
		Date updateTime = new Date();
		bookmark.setUpdateTime( updateTime );

		// TemporalType#time only deals with the time component. Date should be set to zero epoch
		calendar.setTime( updateTime );
		int expectedHour = calendar.get( Calendar.HOUR_OF_DAY );
		int expectedMinute = calendar.get( Calendar.MINUTE );
		int expectedSecond = calendar.get( Calendar.SECOND );
		int expectedTimeZoneOffset = calendar.getTimeZone().getRawOffset();

		Bookmark loadedBookmark = saveAndGet( bookmark );
		calendar.setTime( loadedBookmark.getUpdateTime() );
		int actualHour = calendar.get( Calendar.HOUR_OF_DAY );
		int actualMinute = calendar.get( Calendar.MINUTE );
		int actualSecond = calendar.get( Calendar.SECOND );
		int actualTimeZoneOffset = calendar.getTimeZone().getRawOffset();

		assertEquals( "Hour value does not match", expectedHour, actualHour );
		assertEquals( "Minute value does not match", expectedMinute, actualMinute );
		assertEquals( "Second value does not match", expectedSecond, actualSecond );
		assertEquals( "Time zones doe not match", expectedTimeZoneOffset, actualTimeZoneOffset );
	}

	@Test
	public void testDatePersistedAsTemporalTypeTimestampSupport() throws Exception {
		Date destructionDate = new Date();
		bookmark.setDestructionDate( destructionDate );

		Bookmark loadedBookmark = saveAndGet( bookmark );

		assertEquals( "Year value does not match", bookmark.getDestructionDate(), loadedBookmark.getDestructionDate() );
	}

	@Test
	public void testCalendarTemporalTypeTimestampSupport() throws Exception {
		bookmark.setDestructionCalendar( Calendar.getInstance() );

		Bookmark loadedBookmark = saveAndGet( bookmark );

		assertEquals(
				"Calendar value does not match", bookmark.getDestructionCalendar().getTime(),
				loadedBookmark.getDestructionCalendar().getTime()
		);
	}

	@Test
	public void testCalendarPersistedAsTemporalTypeDateSupport() throws Exception {
		Calendar creationCalendar = Calendar.getInstance();
		bookmark.setCreationCalendar( creationCalendar );

		// TemporalType#Date only deals with year/month/day
		int expectedYear = creationCalendar.get( Calendar.YEAR );
		int expectedMonth = creationCalendar.get( Calendar.MONTH );
		int expectedDay = creationCalendar.get( Calendar.DAY_OF_MONTH );
		int expectedTimeZoneOffset = creationCalendar.getTimeZone().getRawOffset();

		Bookmark loadedBookmark = saveAndGet( bookmark );
		Calendar loadedCalendar = loadedBookmark.getCreationCalendar();
		int actualYear = loadedCalendar.get( Calendar.YEAR );
		int actualMonth = loadedCalendar.get( Calendar.MONTH );
		int actualDay = loadedCalendar.get( Calendar.DAY_OF_MONTH );
		int actualTimeZoneOffset = loadedCalendar.getTimeZone().getRawOffset();

		assertEquals( "Year value does not match", expectedYear, actualYear );
		assertEquals( "Month value does not match", expectedMonth, actualMonth );
		assertEquals( "Day value does not match", expectedDay, actualDay );
		assertEquals( "Time zones doe not match", expectedTimeZoneOffset, actualTimeZoneOffset );
	}

	// Misc
	@Test
	public void testURLSupport() throws Exception {
		bookmark.setUrl( new URL( "http://www.hibernate.org/" ) );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "URL value does not match", bookmark.getUrl(), loadedBookmark.getUrl() );
	}

	@Test
	public void testUUIDSupport() throws Exception {
		UUID serialNumber = UUID.randomUUID();
		bookmark.setSerialNumber( serialNumber );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "UUID value does not match", bookmark.getSerialNumber(), loadedBookmark.getSerialNumber() );
	}

	@Test
	public void testBigDecimalSupport() throws Exception {
		bookmark.setSiteWeight( new BigDecimal( "21.77" ) );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "BigDecimal value does not match", bookmark.getSiteWeight(), loadedBookmark.getSiteWeight() );
	}

	@Test
	public void testBigIntegerSupport() throws Exception {
		bookmark.setVisitCount( new BigInteger( "444" ) );

		Bookmark loadedBookmark = saveAndGet( bookmark );
		assertEquals( "BigInteger value does not match", bookmark.getVisitCount(), loadedBookmark.getVisitCount() );
	}

	private Bookmark saveAndGet(Bookmark bookmark) {
		// persist
		Transaction transaction = session.beginTransaction();
		session.persist( bookmark );
		transaction.commit();

		// making sure the session is cleared
		session.clear();

		// get
		transaction = session.beginTransaction();
		Bookmark retrievedBookmark = (Bookmark) session.get( Bookmark.class, bookmark.getId() );
		transaction.commit();
		return retrievedBookmark;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Bookmark.class
		};
	}
}
