/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.embeddable;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDbObject;

import java.util.Arrays;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.embeddable.Account;
import org.hibernate.ogm.backendtck.embeddable.Address;
import org.hibernate.ogm.backendtck.embeddable.AddressType;
import org.hibernate.ogm.backendtck.queries.AnEmbeddable;
import org.hibernate.ogm.backendtck.queries.AnotherEmbeddable;
import org.hibernate.ogm.backendtck.queries.EmbeddedCollectionItem;
import org.hibernate.ogm.backendtck.queries.WithEmbedded;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Tests for {@code @Embeddable} types and {@code @ElementCollection}s there-of.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
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
		assertDbObject(
				session.getSessionFactory(),
				// collection
				"Account",
				// query
				"{ '_id' : 'emmanuel' }",
				// expected
				"{ " +
					"'_id' : 'emmanuel', " +
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

		Account loadedAccount = (Account) session.get( Account.class, account.getLogin() );

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
				"{ '_id' : 'emmanuel' }",
				// expected
				"{ " +
					"'_id' : 'emmanuel', " +
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

		loadedAccount = (Account) session.get( Account.class, account.getLogin() );

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
				"{ '_id' : 'emmanuel' }",
				// expected
				"{ " +
						"'_id' : 'emmanuel', " +
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

		loadedAccount = (Account) session.get( Account.class, account.getLogin() );

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
				"{ '_id' : 'emmanuel' }",
				// expected
				"{ " +
						"'_id' : 'emmanuel', " +
						"'postal_code' : '75007', " +
						"'version': 3 " +
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
		WithEmbedded with = new WithEmbedded( id, null );
		with.setAnEmbeddable( new AnEmbeddable( "embedded 1", new AnotherEmbeddable( "string 1", 1 ) ) );
		with.setYetAnotherEmbeddable( new AnEmbeddable( "embedded 2" ) );
		with.setAnEmbeddedCollection( Arrays.asList( new EmbeddedCollectionItem( "item[0]", "secondItem[0]", null ), new EmbeddedCollectionItem( "item[1]", null, new AnotherEmbeddable( "string[1][0]", 10 ) ) ) );
		with.setAnotherEmbeddedCollection( Arrays.asList( new EmbeddedCollectionItem( "another[0]", null, null ), new EmbeddedCollectionItem( "another[1]", null, null ) ) );

		session.persist( with );
		transaction.commit();
		session.clear();
		transaction = session.beginTransaction();

		// Then
		assertDbObject( session.getSessionFactory(),
		// collection
				WithEmbedded.class.getSimpleName(),
				// query
				"{ '_id' : " + id + " }",
				// expected
				"{" +
					"'_id' : " + id  + "," +
					"'anEmbeddable' : {" +
							"'anotherEmbeddable' : {" +
								"'embeddedInteger' : 1," +
								"'embeddedString' : 'string 1'" +
							"}," +
							"'embeddedString' : 'embedded 1'" +
					"}," +
					"'yetAnotherEmbeddable' : {" +
							"'embeddedString' : 'embedded 2'" +
					"}," +
					"'anEmbeddedCollection' : [" +
						"{" +
							"'item' : 'item[1]'," +
							"'embedded' : {" +
								"'embeddedString' : 'string[1][0]'," +
								"'embeddedInteger' : 10," +
							"}," +
						"}," +
						"{" +
							"'item' : 'item[0]'," +
							"'anotherItem' : 'secondItem[0]'" +
						"}" +
					"]," +
					"'anotherEmbeddedCollection' : [" +
							"{ 'item' : 'another[1]' }," +
							"{ 'item' : 'another[0]' }" +
					"]" +
				"}"
		);

		session.delete( with );
		transaction.commit();
		session.clear();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Account.class, WithEmbedded.class };
	}
}
