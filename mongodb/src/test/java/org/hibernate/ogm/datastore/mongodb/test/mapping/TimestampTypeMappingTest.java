/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.mapping;

import java.sql.Timestamp;
import java.util.Random;
import java.util.TimeZone;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the timestamp mappings into MongoDB.
 *
 * @author Pavel Novikov
 */
public class TimestampTypeMappingTest extends OgmTestCase {

	private static TimeZone originalTimeZone = null;

	private Session session;
	private TimestampTestEntity testEntity;

	@BeforeClass
	public static void setDefaultTimeZone() {
		originalTimeZone = TimeZone.getDefault();
		TimeZone.setDefault( TimeZone.getTimeZone( "UTC" ) );
	}

	@AfterClass
	public static void resetDefaultTimeZone() {
		TimeZone.setDefault( originalTimeZone );
	}

	@Before
	public void setup() {
		session = openSession();
		testEntity = new TimestampTestEntity();
	}

	// Timestamp type
	@Test
	public void testTimestampPersistedAsDateSupport() throws Exception {
		Timestamp creationDateAndTime = getTimestamp();
		testEntity.setCreationDateAndTime( creationDateAndTime );

		TimestampTestEntity loadedTestEntity = saveAndGet( testEntity );
		Timestamp loadedCreationDate = loadedTestEntity.getCreationDateAndTime();

		assertEquals( "Timestamps does not match", creationDateAndTime, loadedCreationDate );
	}

	private Timestamp getTimestamp() {
		Timestamp timestamp = new Timestamp( System.currentTimeMillis() );
//		Nanos are not supported in MongoDB, that's why they would be 0 always
//		timestamp.setNanos( new Random().nextInt( 999999999 ) );
		return timestamp;
	}

	private TimestampTestEntity saveAndGet(TimestampTestEntity testEntity) {
		// persist
		Transaction transaction = session.beginTransaction();
		session.persist( testEntity );
		transaction.commit();

		// making sure the session is cleared
		session.clear();

		// get
		transaction = session.beginTransaction();
		TimestampTestEntity retrievedBookmark = session.get(
				TimestampTestEntity.class,
				testEntity.getId()
		);
		transaction.commit();
		return retrievedBookmark;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				TimestampTestEntity.class
		};
	}
}