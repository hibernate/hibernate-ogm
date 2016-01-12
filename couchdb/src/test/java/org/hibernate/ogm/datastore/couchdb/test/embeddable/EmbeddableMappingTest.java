/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.test.embeddable;

import static org.hibernate.ogm.datastore.couchdb.utils.CouchDBTestHelper.assertDbObject;

import java.util.Arrays;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.embeddable.Account;
import org.hibernate.ogm.backendtck.embeddable.Address;
import org.hibernate.ogm.backendtck.embeddable.AddressType;
import org.hibernate.ogm.backendtck.queries.Ending;
import org.hibernate.ogm.backendtck.queries.OptionalStoryBranch;
import org.hibernate.ogm.backendtck.queries.StoryBranch;
import org.hibernate.ogm.backendtck.queries.StoryGame;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Tests for {@code @Embeddable} types and {@code @ElementCollection}s there-of.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 * @author Marco Rizzi
 */
public class EmbeddableMappingTest extends OgmTestCase {

	@Test
	public void testEmbeddable() throws Exception {
		OgmSession session = openSession();

		Transaction transaction = session.beginTransaction();

		// Given, When
		Account account = new Account();
		account.setLogin( "emmanuel" );
		account.setPassword( "like I would tell ya" );
		account.setHomeAddress( new Address() );
		final Address address = account.getHomeAddress();
		address.setCity( "Paris" );
		address.setCountry( "France" );
		address.setStreet1( "1 avenue des Champs Elysees" );
		address.setZipCode( "75007" );
		address.setType( new AddressType( "main" ) );
		session.persist( account );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();

		// TODO OGM-893 Redefined column name of embeddable should be prefixed with embeddable name (i.e. 'postal_code' has to be inside 'homeAddress')
		// Then
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Account",
				// query
				"Account:login_:emmanuel_",
				// expected
				"{ " +
					"'login' : 'emmanuel', " +
					"'homeAddress' : {" +
						"'city' : 'Paris', " +
						"'country' : 'France', " +
						"'street1' : '1 avenue des Champs Elysees'," +
						"'type' : {" +
							"'name' : 'main'" +
						"}" +
					"}, " +
					"'postal_code' : '75007', " +
					"'password' : 'like I would tell ya', " +
					"'version': 0 " +
				"}"
		);

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();

		Account loadedAccount = session.get( Account.class, account.getLogin() );

		// When

		// set some values to null
		loadedAccount.getHomeAddress().setCountry( null );
		loadedAccount.setPassword( null );
		session.merge( loadedAccount );

		transaction.commit();

		transaction = session.beginTransaction();

		// Then
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Account",
				// query
				"Account:login_:emmanuel_",
				// expected
				"{ " +
					"'login' : 'emmanuel', " +
					"'homeAddress' : {" +
						"'city' : 'Paris', " +
						"'street1' : '1 avenue des Champs Elysees'," +
						"'type' : {" +
							"'name' : 'main'" +
						"}" +
					"}, " +
					"'postal_code' : '75007', " +
					"'version': 1 " +
				"}"
		);

		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();

		loadedAccount = session.get( Account.class, account.getLogin() );

		// When
		// set a nested embedded to null
		loadedAccount.getHomeAddress().setType( null );
		session.merge( loadedAccount );

		transaction.commit();

		transaction = session.beginTransaction();

		// Then
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Account",
				// query
				"Account:login_:emmanuel_",
				// expected
				"{ " +
					"'login' : 'emmanuel', " +
					"'homeAddress' : {" +
						"'city' : 'Paris', " +
						"'street1' : '1 avenue des Champs Elysees'," +
					"}, " +
					"'postal_code' : '75007', " +
					"'version': 2 " +
				"}"
		);

		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();

		loadedAccount = session.get( Account.class, account.getLogin() );

		// When
		// set all properties of an embedded to null
		loadedAccount.getHomeAddress().setCity( null );
		loadedAccount.getHomeAddress().setStreet1( null );
		session.merge( loadedAccount );

		transaction.commit();

		transaction = session.beginTransaction();

		// Then
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Account",
				// query
				"Account:login_:emmanuel_",
				// expected
				"{ " +
					"'login' : 'emmanuel', " +
					"'postal_code' : '75007', " +
					"'version': 3 " +
				"}"
		);

		transaction.commit();
		// Clean-Up
		transaction = session.beginTransaction();
		loadedAccount = session.get( Account.class, account.getLogin() );
		session.delete( loadedAccount );
		transaction.commit();

		session.close();
	}

	@Test
	public void testNullEmbeddable() throws Exception {
		OgmSession session = openSession();

		Transaction transaction = session.beginTransaction();

		// Given, When
		Account account = new Account();
		account.setLogin( "emmanuel" );
		account.setPassword( "like I would tell ya" );
		account.setHomeAddress( new Address() );
		final Address address = account.getHomeAddress();
		address.setCity( "Paris" );
		address.setCountry( "France" );
		address.setStreet1( "1 avenue des Champs Elysees" );
		address.setZipCode( "75007" );
		address.setType( new AddressType( "main" ) );
		session.persist( account );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();

		// Then
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Account",
				// query
				"Account:login_:emmanuel_",
				// expected
				"{ " +
					"'login' : 'emmanuel', " +
					"'homeAddress' : {" +
						"'city' : 'Paris', " +
						"'country' : 'France', " +
						"'street1' : '1 avenue des Champs Elysees'," +
						"'type' : {" +
							"'name' : 'main'" +
						"}" +
					"}, " +
					"'postal_code' : '75007', " +
					"'password' : 'like I would tell ya', " +
					"'version': 0 " +
				"}"
		);

		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();

		Account loadedAccount = session.get( Account.class, account.getLogin() );
		// set home address (embedded object) to null
		loadedAccount.setHomeAddress( null );
		session.merge( loadedAccount );

		transaction.commit();

		transaction = session.beginTransaction();

		// Then
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Account",
				// query
				"Account:login_:emmanuel_",
				// expected
				"{ " +
					"'login' : 'emmanuel', " +
					"'password' : 'like I would tell ya', " +
					"'version': 1 " +
				"}"
		);

		transaction.commit();

		// Clean-Up
		transaction = session.beginTransaction();
		loadedAccount = (Account) session.get( Account.class, account.getLogin() );
		session.delete( loadedAccount );
		transaction.commit();

		session.close();
	}

	@Test
	public void testEmbeddableCollection() throws Exception {
		OgmSession session = openSession();
		Transaction transaction = session.beginTransaction();

		// Given, When
		// If the value is not big enough, it gets converted as integer
		Long id = Long.MAX_VALUE;
		StoryGame story = new StoryGame( id, null );
		story.setGoodBranch( new StoryBranch( "you go to the village", new Ending( "village ending - everybody is happy", 1 ) ) );
		story.setEvilBranch( new StoryBranch( "you kill the villagers" ) );
		story.setChaoticBranches( Arrays.asList(
				new OptionalStoryBranch( "search the evil [artifact]", "you punish the bandits", null ),
				new OptionalStoryBranch( "assassinate the leader of the party", null, new Ending( "you become a demon", 10 ) ) ) );
		story.setNeutralBranches( Arrays.asList(
				new OptionalStoryBranch( "steal the [artifact]", null, null ),
				new OptionalStoryBranch( "kill the king", null, null ) ) );

		session.persist( story );
		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		// Then
		assertDbObject( session.getSessionFactory(),
		// collection
				StoryGame.class.getSimpleName(),
				// query
				"StoryGame:id_:" + id + "_",
				// expected
				"{" +
					"'id' : '" + id + "'," +
					"'goodBranch' : {" +
						"'ending' : {" +
							"'score' : 1," +
							"'text' : 'village ending - everybody is happy'" +
						"}," +
						"'storyText' : 'you go to the village'" +
					"}," +
					"'evilBranch' : {" +
						"'storyText' : 'you kill the villagers'" +
					"}," +
					"'chaoticBranches' : [" +
						"{" +
							"'evilText' : 'assassinate the leader of the party'," +
							"'evilEnding': {" +
								"'text' : 'you become a demon'," +
								"'score' : 10," +
							"}" +
						"}," +
						"{" +
							"'evilText' : 'search the evil [artifact]'," +
							"'goodText' : 'you punish the bandits'" +
						"}" +
					"]," +
					"'neutralBranches' : [" +
						"{ 'evilText' : 'steal the [artifact]' }," +
						"{ 'evilText' : 'kill the king' }" +
					"]" +
				"}"
		);

		session.delete( story );
		transaction.commit();
		session.clear();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Account.class, StoryGame.class };
	}
}
