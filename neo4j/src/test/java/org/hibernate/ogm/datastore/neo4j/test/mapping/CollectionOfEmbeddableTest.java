/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.embeddable.Address;
import org.hibernate.ogm.backendtck.embeddable.MultiAddressAccount;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class CollectionOfEmbeddableTest extends Neo4jJpaTestCase {

	private Address address;
	private Address anotherAddress;
	private MultiAddressAccount account;

	@Before
	public void prepareDB() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();

		address = new Address();
		address.setCity( "Paris" );
		address.setCountry( "France" );
		address.setStreet1( "1 avenue des Champs Elysees" );
		address.setZipCode( "75007" );

		anotherAddress = new Address();
		anotherAddress.setCity( "Rome" );
		anotherAddress.setCountry( "Italy" );
		anotherAddress.setStreet1( "Piazza del Colosseo, 1" );
		anotherAddress.setZipCode( "00184" );

		account = new MultiAddressAccount();
		account.setLogin( "gunnar" );
		account.setPassword( "highly secret" );
		account.getAddresses().add( address );
		account.getAddresses().add( anotherAddress );

		em.persist( account );
		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 3 );
		assertRelationships( 2 );

		String accountNode = "(:MultiAddressAccount:ENTITY {"
				+ "  `login`: '" + account.getLogin() + "'"
				+ ", `password`: '" + account.getPassword() + "'"
				+ " })";

		String addressNode = "(:MultiAddressAccount_addresses:EMBEDDED {"
				+ "  street1: '" + address.getStreet1() + "'"
				+ ", city: '" + address.getCity() + "'"
				+ ", country: '" + address.getCountry() + "'"
				+ ", postal_code: '" + address.getZipCode() + "'"
				+ " })";

		String anotherAddressNode = "(:MultiAddressAccount_addresses:EMBEDDED {"
				+ "  street1: '" + anotherAddress.getStreet1() + "'"
				+ ", city: '" + anotherAddress.getCity() + "'"
				+ ", country: '" + anotherAddress.getCountry() + "'"
				+ ", postal_code: '" + anotherAddress.getZipCode() + "'"
				+ " })";

		assertExpectedMapping( accountNode + " - [:addresses] -> " + addressNode );
		assertExpectedMapping( accountNode + " - [:addresses] -> " + anotherAddressNode );
	}

	@Test
	public void testNoNodeIsLeftBehindWhenDeletingRelationships() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();
		MultiAddressAccount multiAddressAccount = em.find( MultiAddressAccount.class, account.getLogin() );
		multiAddressAccount.getAddresses().clear();
		commitOrRollback( true );
		em.close();

		assertNumberOfNodes( 1 );
		assertRelationships( 0 );

		String accountNode = "(:MultiAddressAccount:ENTITY {"
				+ "  `login`: '" + account.getLogin() + "'"
				+ ", `password`: '" + account.getPassword() + "'"
				+ " })";

		assertExpectedMapping( accountNode );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { MultiAddressAccount.class, Address.class };
	}

}
