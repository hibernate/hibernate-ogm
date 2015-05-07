/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

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
		value = { GridDialectType.CASSANDRA },
		comment = "MultiAddressAccount.addresses list - bag semantics unsupported (no primary key)"
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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { MultiAddressAccount.class };
	}
}
