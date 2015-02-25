/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.embeddable;

import static org.hibernate.ogm.datastore.mongodb.utils.MongoDBTestHelper.assertDbObject;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.embeddable.Account;
import org.hibernate.ogm.backendtck.embeddable.Address;
import org.hibernate.ogm.backendtck.embeddable.AddressType;
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Account.class };
	}
}
