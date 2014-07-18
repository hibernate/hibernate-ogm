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
		assertNumberOfNodes( 1 );
		assertRelationships( 0 );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "login", account.getLogin() );
		properties.put( "password", account.getPassword());
		properties.put( "homeAddress.street1", account.getHomeAddress().getStreet1() );
		properties.put( "homeAddress.city", account.getHomeAddress().getCity() );
		properties.put( "homeAddress.country", account.getHomeAddress().getCountry() );
		properties.put( "postal_code", account.getHomeAddress().getZipCode() );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "n", properties );

		assertExpectedMapping( "n", "(n:Account:ENTITY {"
				+ "  `login`: {n}.login"
				+ ", `password`: {n}.password"
				+ ", `homeAddress.street1`: {n}.`homeAddress.street1`"
				+ ", `homeAddress.city`: {n}.`homeAddress.city`"
				+ ", `homeAddress.country`: {n}.`homeAddress.country`"
				+ ", `postal_code`: {n}.postal_code"
				+ " })", params );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { Account.class, Address.class };
	}

}
