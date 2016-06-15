/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Tests for {@code @Embeddable} types and {@code @ElementCollection}s there-of.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public class EmbeddableTest extends OgmTestCase {

	@Test
	public void testEmbeddable() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();
		Account account = new Account();
		account.setLogin( "emmanuel" );
		account.setPassword( "like I would tell ya" );
		account.setHomeAddress( new Address() );
		final Address address = account.getHomeAddress();
		address.setCity( "Paris" );
		address.setCountry( "France" );
		address.setStreet1( "1 avenue des Champs Elysees" );
		address.setZipCode( "75007" );
		session.persist( account );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		final Account loadedAccount = (Account) session.get( Account.class, account.getLogin() );
		assertThat( loadedAccount ).as( "Cannot load persisted object" ).isNotNull();
		final Address loadedAddress = loadedAccount.getHomeAddress();
		assertThat( loadedAddress ).as( "Embeddable should not be null" ).isNotNull();
		assertThat( loadedAddress.getCity() ).as( "persist and load fails for embeddable" ).isEqualTo( address.getCity() );
		assertThat( loadedAddress.getZipCode() ).as( "@Column support for embeddable does not work" ).isEqualTo( address.getZipCode() );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		loadedAddress.setCountry( "USA" );
		session.merge( loadedAccount );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		Account secondLoadedAccount = (Account) session.get( Account.class, account.getLogin() );
		assertThat( loadedAccount.getHomeAddress().getCity() ).as( "Merge fails for embeddable" ).isEqualTo( secondLoadedAccount.getHomeAddress().getCity() );
		session.delete( secondLoadedAccount );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		assertThat( session.get( Account.class, account.getLogin() ) ).isNull();
		transaction.commit();

		session.close();
	}

	@Test
	public void testNestedEmbeddable() {
		final Session session = openSession();

		// persist entity without the embeddables
		Transaction transaction = session.beginTransaction();
		Account account = new Account();
		account.setLogin( "gunnar" );
		session.persist( account );

		transaction.commit();
		session.clear();

		// read back
		transaction = session.beginTransaction();
		Account loadedAccount = (Account) session.get( Account.class, account.getLogin() );
		assertThat( loadedAccount ).as( "Cannot load persisted object with nested embeddables which are null" ).isNotNull();
		assertThat( loadedAccount.getHomeAddress() ).isNull();

		// update
		loadedAccount.setHomeAddress( new Address() );
		loadedAccount.getHomeAddress().setCity( "Lima" );
		loadedAccount.getHomeAddress().setType( new AddressType( "primary" ) );

		transaction.commit();
		session.clear();

		// read back nested embeddable
		transaction = session.beginTransaction();
		loadedAccount = (Account) session.get( Account.class, account.getLogin() );
		assertThat( loadedAccount ).as( "Cannot load persisted object with nested embeddables" ).isNotNull();
		assertThat( loadedAccount.getHomeAddress() ).isNotNull();
		assertThat( loadedAccount.getHomeAddress().getCity() ).isEqualTo( "Lima" );
		assertThat( loadedAccount.getHomeAddress().getType() ).isNotNull();
		assertThat( loadedAccount.getHomeAddress().getType().getName() ).isEqualTo( "primary" );

		session.delete( loadedAccount );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		assertThat( session.get( Account.class, account.getLogin() ) ).isNull();
		transaction.commit();

		session.close();
	}


	@Test
	public void testNestedEmbeddedWithNullProperties() {
		final Session session = openSession();

		// persist entity with the embeddables
		Transaction transaction = session.beginTransaction();
		Account account = new Account();
		account.setLogin( "gunnar" );
		account.setHomeAddress( new Address() );
		account.getHomeAddress().setCity( "Lima" );
		account.getHomeAddress().setType( new AddressType( "primary" ) );
		session.persist( account );

		transaction.commit();
		session.clear();


		// set nested embedded to null
		transaction = session.beginTransaction();
		Account loadedAccount = (Account) session.get( Account.class, account.getLogin() );
		loadedAccount.getHomeAddress().setType( null );
		transaction.commit();
		session.clear();

		// read back nested embedded and set regular embedded to null
		transaction = session.beginTransaction();
		loadedAccount = (Account) session.get( Account.class, account.getLogin() );
		assertThat( loadedAccount ).as( "Cannot load persisted object with nested embeddables" ).isNotNull();
		assertThat( loadedAccount.getHomeAddress() ).isNotNull();
		assertThat( loadedAccount.getHomeAddress().getType() ).isNull();
		loadedAccount.setHomeAddress( null );
		transaction.commit();
		session.clear();

		// read back embedded
		transaction = session.beginTransaction();
		loadedAccount = (Account) session.get( Account.class, account.getLogin() );
		assertThat( loadedAccount ).as( "Cannot load persisted object with nested embeddables" ).isNotNull();
		assertThat( loadedAccount.getHomeAddress() ).isNull();
		session.delete( loadedAccount );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		assertThat( session.get( Account.class, account.getLogin() ) ).isNull();
		transaction.commit();

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Account.class };
	}
}
