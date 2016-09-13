/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for {@code @Embeddable} types and {@code @ElementCollection}s there-of.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
@SkipByGridDialect(
		value = { GridDialectType.CASSANDRA, GridDialectType.INFINISPAN_REMOTE },
		comment = "POJOs contain lists - bag semantics unsupported (no primary key)"
)
public class EmbeddableExtraTest extends OgmTestCase {

	@Test
	public void testElementCollectionOfEmbeddable() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();

		Address address = new Address();
		address.setCity( "Paris" );
		address.setCountry( "France" );
		address.setStreet1( "1 avenue des Champs Elysees" );
		address.setZipCode( "75007" );

		Address anotherAddress = new Address();
		anotherAddress.setCity( "Rome" );
		anotherAddress.setCountry( "Italy" );
		anotherAddress.setStreet1( "Piazza del Colosseo, 1" );
		anotherAddress.setZipCode( "00184" );
		anotherAddress.setType( new AddressType( "primary" ) );

		MultiAddressAccount account = new MultiAddressAccount();
		account.setLogin( "gunnar" );
		account.setPassword( "highly secret" );
		account.getAddresses().add( address );
		account.getAddresses().add( anotherAddress );

		session.persist( account );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		MultiAddressAccount loadedAccount = (MultiAddressAccount) session.get( MultiAddressAccount.class, account.getLogin() );
		assertThat( loadedAccount ).as( "Cannot load persisted object" ).isNotNull();
		assertThat( loadedAccount.getAddresses() ).onProperty( "city" ).containsOnly( "Paris", "Rome" );
		assertThat( loadedAccount.getAddresses() ).onProperty( "zipCode" ).containsOnly( "75007", "00184" );
		assertThat( loadedAccount.getAddresses() ).onProperty( "country" ).containsOnly( "France", "Italy" );
		assertThat( loadedAccount.getAddresses() ).onProperty( "street2" ).containsOnly( null, null );
		assertThat( loadedAccount.getAddresses() ).onProperty( "type" ).containsOnly( new AddressType( "primary" ), null );

		Address loadedAddress1 = loadedAccount.getAddresses().get( 0 );
		Address loadedAddress2 = loadedAccount.getAddresses().get( 1 );

		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		loadedAddress1.setCountry( "USA" );
		loadedAddress2.setCountry( "Germany" );

		session.merge( loadedAccount );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		MultiAddressAccount secondLoadedAccount = (MultiAddressAccount) session.get( MultiAddressAccount.class, account.getLogin() );
		assertThat( secondLoadedAccount.getAddresses() ).onProperty( "city" ).contains( "Paris", "Rome" );
		assertThat( secondLoadedAccount.getAddresses() ).onProperty( "country" ).contains( "USA", "Germany" );
		session.delete( secondLoadedAccount );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		assertThat( session.get( MultiAddressAccount.class, account.getLogin() ) ).isNull();
		transaction.commit();

		session.close();
	}

	@Test
	public void testPersistEmbeddedWithNullEmbeddedList() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();

		AccountWithPhone wombatSoftware = new AccountWithPhone( "1", "Mobile account 1" );
		wombatSoftware.setPhoneNumber( null );

		session.persist( wombatSoftware );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		AccountWithPhone loadedUser = (AccountWithPhone) session.get( AccountWithPhone.class, wombatSoftware.getId() );
		assertThat( loadedUser ).as( "Cannot load persisted object with nested embeddables" ).isNotNull();
		// It is not null because of the list of elements
		assertThat( loadedUser.getPhoneNumber() ).isNotNull();
		assertThat( loadedUser.getPhoneNumber().getMain() ).isNull();
		assertThat( loadedUser.getPhoneNumber().getAlternatives() ).isEmpty();

		session.delete( loadedUser );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		assertThat( session.get( AccountWithPhone.class, wombatSoftware.getId() ) ).isNull();
		transaction.commit();
		session.close();
	}

	@Test
	public void testPersistWithEmbeddedList() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();
		List<String> alternativePhones = Arrays.asList( "+1-222-555-0222", "+1-202-555-0333" );
		AccountWithPhone account = new AccountWithPhone( "2", "Mobile account 2" );
		account.setPhoneNumber( new PhoneNumber( "+1-222-555-0111", alternativePhones ) );

		session.persist( account );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		AccountWithPhone loadedUser = (AccountWithPhone) session.get( AccountWithPhone.class, account.getId() );
		assertThat( loadedUser ).as( "Cannot load persisted object with nested embeddables" ).isNotNull();
		assertThat( loadedUser.getPhoneNumber() ).isNotNull();
		assertThat( loadedUser.getPhoneNumber().getMain() ).isEqualTo( account.getPhoneNumber().getMain() );
		assertThat( loadedUser.getPhoneNumber().getAlternatives() ).containsOnly(
				alternativePhones.toArray(
						new Object[alternativePhones.size()]
				)
		);

		session.delete( loadedUser );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		assertThat( session.get( AccountWithPhone.class, account.getId() ) ).isNull();
		transaction.commit();
		session.close();
	}

	@Test
	public void testPersistWithListEmbeddedInNestedComponent() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();

		Order order = new Order(
				"order-1",
				"Telescope",
				new ShippingAddress(
						new PhoneNumber(  "+1-222-555-0111", Arrays.asList( "+1-222-555-0222", "+1-202-555-0333" ) ),
						"Planet road 68"
				)
		);

		session.persist( order );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		Order loadedOrder = (Order) session.get( Order.class, "order-1" );
		assertThat( loadedOrder ).as( "Cannot load persisted object with nested embeddables" ).isNotNull();
		assertThat( loadedOrder.getShippingAddress() ).isNotNull();
		assertThat( loadedOrder.getShippingAddress().getPhone() ).isNotNull();
		assertThat( loadedOrder.getShippingAddress().getPhone().getMain() ).isEqualTo( "+1-222-555-0111" );
		assertThat( loadedOrder.getShippingAddress().getPhone().getAlternatives() ).containsOnly( "+1-222-555-0222", "+1-202-555-0333" );

		session.delete( loadedOrder );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		assertThat( session.get( Order.class, "order-1" ) ).isNull();
		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MultiAddressAccount.class, AccountWithPhone.class, Order.class };
	}
}
