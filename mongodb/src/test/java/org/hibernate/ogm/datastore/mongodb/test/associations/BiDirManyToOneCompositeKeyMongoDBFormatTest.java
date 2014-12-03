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
import org.hibernate.ogm.backendtck.associations.compositeid.Court;
import org.hibernate.ogm.backendtck.associations.compositeid.Game;
import org.hibernate.ogm.utils.OgmTestCase;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDbObject;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class BiDirManyToOneCompositeKeyMongoDBFormatTest extends OgmTestCase {

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
						"[ { 'id.sequenceNo': 456, 'id.category': 'primary' }, " +
						" { 'id.sequenceNo': 457, 'id.category': 'primary' } ], " +
						"'name': 'Hamburg Court' " +
				"}"
		);

		Court localCourt = (Court) session.get( Court.class, new Court.CourtId( "DE", 123 ) );
		for ( Game game : localCourt.getGames() ) {
			session.delete( game );
		}
		localCourt.getGames().clear();
		session.delete( localCourt );
		transaction.commit();

		session.close();
	}
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Game.class, Court.class };
	}
}
