/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.associations;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.backendtck.associations.manytoone.Court;
import org.hibernate.ogm.backendtck.associations.manytoone.Game;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.Test;

import com.lambdaworks.redis.RedisConnection;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Mark Paluch
 */
public class ManyToOneInEntityJsonRepresentationTest extends OgmTestCase {


	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put(
				DocumentStoreProperties.ASSOCIATIONS_STORE,
				AssociationStorageType.IN_ENTITY
		);
	}

	// See OGM-879
	@Test
	public void testDefaultBiDirManyToOneCompositeKeyTest() throws Exception {

		//given
		Session session = openSession();
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


		// when
		String representation = new String(
				getConnection().get(
						"Court:{\"id.countryCode\":\"DE\",\"id.sequenceNo\":123}".getBytes()
				)
		);

		// then
		JSONAssert.assertEquals(
				"{\"games\":[{\"id.gameSequenceNo\":456,\"id.category\":\"primary\"}," +
						"{\"id.gameSequenceNo\":457,\"id.category\":\"primary\"}]," +
						"\"name\":\"Hamburg Court\"}",
				representation,
				JSONCompareMode.NON_EXTENSIBLE
		);


		session.clear();

		transaction = session.beginTransaction();
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

	protected RedisConnection<byte[], byte[]> getConnection() {
		return getProvider().getConnection();
	}


	private RedisDatastoreProvider getProvider() {
		return (RedisDatastoreProvider) sfi()
				.getServiceRegistry()
				.getService( DatastoreProvider.class );
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Game.class,
				Court.class
		};
	}
}
