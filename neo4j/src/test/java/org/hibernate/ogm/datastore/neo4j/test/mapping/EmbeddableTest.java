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

import org.hibernate.ogm.backendtck.embeddable.Account;
import org.hibernate.ogm.backendtck.embeddable.Address;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class EmbeddableTest extends Neo4jJpaTestCase {

	private Account account;
	private Address address;

	@Before
	public void preapreDB() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		account = new Account();
		account.setLogin( "emmanuel" );
		account.setPassword( "like I would tell ya" );
		account.setHomeAddress( new Address() );

		address = account.getHomeAddress();
		address.setCity( "Paris" );
		address.setCountry( "France" );
		address.setStreet1( "1 avenue des Champs Elysees" );
		address.setZipCode( "75007" );

		em.persist( account );
		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 2 );
		assertRelationships( 1 );

		String accountNode = "(a:Account:ENTITY { login: {a}.login, password: {a}.password, version: {a}.version, postal_code: {a}.postal_code })";
		String addressNode = "(e:EMBEDDED {street1: {e}.street1, city: {e}.city, country: {e}.country})";

		Map<String, Object> accountProperties = new HashMap<String, Object>();
		accountProperties.put( "login", account.getLogin() );
		accountProperties.put( "password", account.getPassword());
		accountProperties.put( "version", account.getVersion() );

		// see OGM-673: At the moment the @Column annotation will make the value stored in the owner node
		// unless you define it as @Column("homeAddress.postal_code")
		accountProperties.put( "postal_code", account.getHomeAddress().getZipCode() );

		Map<String, Object> addressProperties = new HashMap<String, Object>();
		addressProperties.put( "street1", account.getHomeAddress().getStreet1() );
		addressProperties.put( "city", account.getHomeAddress().getCity() );
		addressProperties.put( "country", account.getHomeAddress().getCountry() );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "a", accountProperties);
		params.put( "e", addressProperties );

		assertExpectedMapping( "a", accountNode, params );
		assertExpectedMapping( "e", addressNode, params );
		assertExpectedMapping( "r", accountNode + " - [r:homeAddress] - " + addressNode, params );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { Account.class, Address.class };
	}
}
