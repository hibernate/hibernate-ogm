/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.embeddable.Address;
import org.hibernate.ogm.backendtck.embeddable.MultiAddressAccount;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class CollectionOfEmbeddableTest extends Neo4jJpaTestCase {

	private static final String ACCOUNT_NODE = "(ac:MultiAddressAccount:ENTITY { login: {ac}.login, password: {ac}.password })";

	private static final String ADDRESS_NODE = "(ad:MultiAddressAccount_addresses:EMBEDDED {"
			+ " street1: {ad}.street1"
			+ ", city: {ad}.city"
			+ ", country: {ad}.country"
			+ ", postal_code: {ad}.postal_code"
			+ " })";

	private static final String RELATIONSHIP = ACCOUNT_NODE + " - [r:addresses] - " + ADDRESS_NODE;

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

		assertExpectedMapping( "ac", ACCOUNT_NODE, params( account ) );
		assertExpectedMapping( "ad", ADDRESS_NODE, params( address ) );
		assertExpectedMapping( "ad", ADDRESS_NODE, params( anotherAddress ) );

		assertExpectedMapping( "r", RELATIONSHIP, params( account, address ) );
		assertExpectedMapping( "r", RELATIONSHIP, params( account, anotherAddress ) );
	}

	private Map<String, Object> params(MultiAddressAccount account, Address address) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.putAll( params( account ) );
		params.putAll( params( address ) );
		return params;
	}

	private Map<String, Object> params(MultiAddressAccount account) {
		Map<String, Object> accountProperties = new HashMap<String, Object>();
		accountProperties.put( "login", account.getLogin() );
		accountProperties.put( "password", account.getPassword() );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "ac", accountProperties );
		return params;
	}


	private Map<String, Object> params(Address address) {
		Map<String, Object> addressProperties = new HashMap<String, Object>();
		addressProperties.put( "street1", address.getStreet1() );
		addressProperties.put( "city", address.getCity() );
		addressProperties.put( "country", address.getCountry() );
		addressProperties.put( "postal_code", address.getZipCode() );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "ad", addressProperties );
		return params;
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

		assertExpectedMapping( "ac", ACCOUNT_NODE, params( account ) );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { MultiAddressAccount.class, Address.class };
	}

}
