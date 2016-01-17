/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.test.embeddable;

import static org.hibernate.ogm.datastore.cassandra.utils.CassandraTestHelper.rowAssertion;

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
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for {@code @Embeddable} types and {@code @ElementCollection}s there-of.
 *
 * @author Nicola Ferraro
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

		// Then
		rowAssertion( session.getSessionFactory(), "Account" )
				.keyColumn( "login", "emmanuel" )
				.assertColumn( "login", "emmanuel" )
				.assertColumn( "homeAddress.city", "Paris" )
				.assertColumn( "homeAddress.country", "France" )
				.assertColumn( "homeAddress.street1", "1 avenue des Champs Elysees" )
				.assertColumn( "homeAddress.type.name", "main" )
				.assertColumn( "postal_code", "75007" )
				.assertColumn( "password", "like I would tell ya" )
				.assertColumn( "version", 0 )
				.assertNoOtherColumnPresent()
				.execute();

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
		rowAssertion( session.getSessionFactory(), "Account" )
				.keyColumn( "login", "emmanuel" )
				.assertColumn( "login", "emmanuel" )
				.assertColumn( "homeAddress.city", "Paris" )
				.assertColumn( "homeAddress.street1", "1 avenue des Champs Elysees" )
				.assertColumn( "homeAddress.type.name", "main" )
				.assertColumn( "postal_code", "75007" )
				.assertColumn( "version", 1 )
				.assertNoOtherColumnPresent()
				.execute();

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
		rowAssertion( session.getSessionFactory(), "Account" )
				.keyColumn( "login", "emmanuel" )
				.assertColumn( "homeAddress.city", "Paris" )
				.assertColumn( "homeAddress.street1", "1 avenue des Champs Elysees" )
				.assertColumn( "postal_code", "75007" )
				.assertColumn( "version", 2 )
				.assertNoOtherColumnPresent()
				.execute();

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
		rowAssertion( session.getSessionFactory(), "Account" )
				.keyColumn( "login", "emmanuel" )
				.assertColumn( "login", "emmanuel" )
				.assertColumn( "postal_code", "75007" )
				.assertColumn( "version", 3 )
				.assertNoOtherColumnPresent()
				.execute();

		transaction.commit();
		// Clean-Up
		transaction = session.beginTransaction();
		loadedAccount = session.get( Account.class, account.getLogin() );
		session.delete( loadedAccount );
		transaction.commit();

		session.close();
	}

	@Test
	@Ignore("@ElementCollection seems not to work in Cassandra if no @OrderColumn is specified")
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
		// TODO test the expected mapping

		session.delete( story );
		transaction.commit();
		session.clear();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Account.class, StoryGame.class, OptionalStoryBranch.class };
	}
}
