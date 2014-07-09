/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.query.NoSQLQuery;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Tests for mapping table-based id generators to MongoDB.
 *
 * @author Gunnar Morling
 *
 */
public class TableGeneratorTest extends OgmTestCase {

	@Test
	public void canUseSpecificValueColumNames() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		PianoPlayer ken = new PianoPlayer( "Ken Gold" );
		GuitarPlayer buck = new GuitarPlayer( "Buck Cherry" );
		// when
		session.persist( ken );
		session.persist( buck );

		tx.commit();
		session.clear();
		tx = session.beginTransaction();
		ken = (PianoPlayer) session.load( PianoPlayer.class, ken.getId() );
		buck = (GuitarPlayer) session.load( GuitarPlayer.class, buck.getId() );

		// then
		assertThat( ken.getId() ).isEqualTo( 1L );
		assertThat( buck.getId() ).isEqualTo( 1L );
		assertCountQueryResult( session, "db.PianoPlayerSequence.count( { '_id' : 'pianoPlayer', 'nextPianoPlayerId' : 2 } )", 1 );
		assertCountQueryResult( session, "db.GuitarPlayerSequence.count( { '_id' : 'guitarPlayer', 'nextGuitarPlayerId' : 2 } )", 1 );

		tx.commit();
		session.close();
	}

	private void assertCountQueryResult(OgmSession session, String queryString, long expectedCount) {
		NoSQLQuery query = session.createNativeQuery( queryString );
		query.addScalar( "n" );
		long actualCount = (Long) query.list().iterator().next();
		assertThat( actualCount ).describedAs( "Count query didn't yield expected result" ).isEqualTo( expectedCount );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PianoPlayer.class, GuitarPlayer.class };
	}
}
