/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.test.id;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.cassandra.utils.CassandraTestHelper.rowAssertion;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.id.GuitarPlayer;
import org.hibernate.ogm.backendtck.id.PianoPlayer;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Tests for mapping table-based id generators to Cassandra.
 *
 * @author Nicola Ferraro
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
		ken = session.load( PianoPlayer.class, ken.getId() );
		buck = session.load( GuitarPlayer.class, buck.getId() );

		// then
		assertThat( ken.getId() ).isEqualTo( 1L );
		assertThat( buck.getId() ).isEqualTo( 1L );

		rowAssertion( session.getSessionFactory(), "PianoPlayerSequence" )
				.keyColumn( "sequence_name", "pianoPlayer" )
				.assertColumn( "nextPianoPlayerId", 2L )
				.execute();

		rowAssertion( session.getSessionFactory(), "GuitarPlayerSequence" )
				.keyColumn( "sequence_name", "guitarPlayer" )
				.assertColumn( "nextGuitarPlayerId", 2L )
				.execute();

		tx.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ PianoPlayer.class, GuitarPlayer.class };
	}
}
