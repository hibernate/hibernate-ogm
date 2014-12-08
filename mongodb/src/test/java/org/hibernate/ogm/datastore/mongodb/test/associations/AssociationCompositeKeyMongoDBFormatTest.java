/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.mongodb.test.associations;

import org.junit.Test;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.associations.collection.types.Race;
import org.hibernate.ogm.backendtck.associations.collection.types.Runner;
import org.hibernate.ogm.backendtck.associations.manytoone.Court;
import org.hibernate.ogm.backendtck.associations.manytoone.Game;
import org.hibernate.ogm.utils.OgmTestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDbObject;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class AssociationCompositeKeyMongoDBFormatTest extends OgmTestCase {

	@Test
	public void testDefaultBiDirManyToOneCompositeKeyTest() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();
		Court court = new Court();
		court.setId( new Court.CourtId() );
		court.getId().setCountryCode( "DE" );
		court.getId().setSequenceNo( 123 );
		court.setName( "Hamburg Court" );
		session.persist( court );
		Game game1 = new Game();
		game1.setId( new Game.GameId() );
		game1.getId().setCategory( "primary" );
		game1.getId().setSequenceNo( 456 );
		game1.setName( "The game" );
		game1.setPlayedOn( court );
		court.getGames().add( game1 );
		Game game2 = new Game();
		game2.setId( new Game.GameId() );
		game2.getId().setCategory( "primary" );
		game2.getId().setSequenceNo( 457 );
		game2.setName( "The other game" );
		game2.setPlayedOn( court );
		session.persist( game1 );
		session.persist( game2 );
		session.flush();
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();


		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Court",
				// query
				"{ '_id' : { 'countryCode': 'DE', 'sequenceNo': 123 } }",
				// expected
				"{ " +
						"'_id' : { 'countryCode': 'DE', 'sequenceNo': 123 }, " +
						"'games' : " +
						"[ { 'gameSequenceNo': 456, 'category': 'primary' }, " +
						"  { 'gameSequenceNo': 457, 'category': 'primary' } ], " +
						"'name': 'Hamburg Court' " +
						"}"
		);
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Game",
				// query
				"{ '_id' : { 'category': 'primary', 'gameSequenceNo': 456 } }",
				// expected
				"{ " +
						"'_id' : { 'category': 'primary', 'gameSequenceNo': 456 }, " +
						"'playedOn_id' : { 'countryCode': 'DE', 'sequenceNo': 123 }, " +
						"'name': 'The game' " +
				"}"
		);

		Court localCourt = (Court) session.get( Court.class, new Court.CourtId( "DE", 123 ) );
		assertThat( localCourt.getGames() ).hasSize( 2 );
		for ( Game game : localCourt.getGames() ) {
			session.delete( game );
		}
		localCourt.getGames().clear();
		session.delete( localCourt );
		transaction.commit();

		session.close();
	}

	@Test
	public void testOrderedListAndCompositeId() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();
		Race race = new Race();
		race.setRaceId( new Race.RaceId( 23, 75 ) );
		Runner runner = new Runner();
		runner.setAge( 37 );
		runner.setRunnerId( new Runner.RunnerId( "Emmanuel", "Bernard" ) );
		Runner runner2 = new Runner();
		runner2.setAge( 105 );
		runner2.setRunnerId( new Runner.RunnerId( "Pere", "Noel" ) );
		race.getRunnersByArrival().add( runner );
		race.getRunnersByArrival().add( runner2 );
		session.persist( race );
		session.persist( runner );
		session.persist( runner2 );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Race",
				// query
				"{ '_id' : { 'federationDepartment' : 75, 'federationSequence' : 23 } }",
				// expected
				"{ '_id' : { 'federationDepartment' : 75, 'federationSequence' : 23 }, " +
						"'runnersByArrival' : [ " +
						"{ 'firstname' : 'Emmanuel', 'lastname' : 'Bernard', 'ranking' : 0 }, " +
						"{ 'firstname' : 'Pere', 'lastname' : 'Noel', 'ranking' : 1 } " +
						"] }"
		);
		race = (Race) session.get( Race.class, race.getRaceId() );
		assertThat( race.getRunnersByArrival() ).hasSize( 2 );
		assertThat( race.getRunnersByArrival().get( 0 ).getRunnerId().getFirstname() ).isEqualTo( "Emmanuel" );
		session.delete( race.getRunnersByArrival().get( 0 ) );
		session.delete( race.getRunnersByArrival().get( 1 ) );
		session.delete( race );
		transaction.commit();

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Game.class, Court.class, Race.class, Runner.class };
	}
}
