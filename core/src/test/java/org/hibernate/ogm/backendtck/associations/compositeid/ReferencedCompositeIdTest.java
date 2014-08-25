/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.compositeid;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Tests for using composite keys in associations.
 *
 * @author Gunnar Morling
 */
public class ReferencedCompositeIdTest extends OgmTestCase {

	@Test
	public void testManyToOneReferenceWithCompositeId() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Tournament britishOpen = new Tournament( new TournamentId( "US", "123" ), "British Open" );
		Tournament playersChampionship = new Tournament( new TournamentId( "US", "456" ), "Player's Championship" );
		session.persist( britishOpen );
		session.persist( playersChampionship );

		// persist object with association
		Director bob = new Director( "bob", "Bob", playersChampionship );
		session.persist( bob );

		transaction.commit();
		session.clear();

		// assert and un-set the association
		transaction = session.beginTransaction();

		Director loadedParticipant = (Director) session.get( Director.class, "bob" );
		assertThat( bob.getDirectedTournament().getName() ).isEqualTo( "Player's Championship" );
		bob.setDirectedTournament( null );

		transaction.commit();
		session.clear();

		// association should have been removed
		transaction = session.beginTransaction();

		loadedParticipant = (Director) session.get( Director.class, "bob" );
		assertThat( bob.getDirectedTournament() ).isNull();

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		loadedParticipant = (Director) session.get( Director.class, "bob" );
		session.delete( loadedParticipant.getDirectedTournament() );
		session.delete( loadedParticipant );
		session.delete( session.get( Tournament.class, britishOpen.getId() ) );

		transaction.commit();
	}

	@Test
	public void testManyToManyReferenceWithCompositeId() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Tournament britishOpen = new Tournament( new TournamentId( "US", "123" ), "British Open" );
		Tournament playersChampionship = new Tournament( new TournamentId( "US", "456" ), "Player's Championship" );
		session.persist( britishOpen );
		session.persist( playersChampionship );

		// persist object with association
		Director bob = new Director( "bob", "Bob", null );
		bob.getAttendedTournaments().add( britishOpen );
		bob.getAttendedTournaments().add( playersChampionship );
		session.persist( bob );

		transaction.commit();
		session.clear();

		// assert association, remove one element
		transaction = session.beginTransaction();

		Director loadedParticipant = (Director) session.get( Director.class, "bob" );
		assertThat( bob.getAttendedTournaments() ).onProperty( "name" ).contains( "British Open", "Player's Championship" );
		loadedParticipant.getAttendedTournaments().remove( session.get( Tournament.class, britishOpen.getId() ) );
		transaction.commit();
		session.clear();

		// element should have been removed
		transaction = session.beginTransaction();

		loadedParticipant = (Director) session.get( Director.class, "bob" );
		assertThat( bob.getAttendedTournaments() ).onProperty( "name" ).contains( "Player's Championship" );

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		loadedParticipant = (Director) session.get( Director.class, "bob" );
		session.delete( loadedParticipant );
		session.delete( session.get( Tournament.class, britishOpen.getId() ) );
		session.delete( session.get( Tournament.class, playersChampionship.getId() ) );

		transaction.commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Director.class, Tournament.class, TournamentId.class };
	}
}
