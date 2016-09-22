/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;


import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.EMBEDDED;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.embeddable.Address;
import org.hibernate.ogm.backendtck.embeddable.MultiAddressAccount;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
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
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

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
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions accountNode = node( "account", MultiAddressAccount.class.getSimpleName(), ENTITY.name() )
				.property( "login", account.getLogin() )
				.property( "password", account.getPassword() );

		NodeForGraphAssertions addressNode = node( "address", "MultiAddressAccount_addresses", EMBEDDED.name() )
				.property( "city", address.getCity() )
				.property( "country", address.getCountry() )
				.property( "street1", address.getStreet1() )
				.property( "postal_code", address.getZipCode() );

		NodeForGraphAssertions anotherNode = node( "another", "MultiAddressAccount_addresses", EMBEDDED.name() )
				.property( "city", anotherAddress.getCity() )
				.property( "country", anotherAddress.getCountry() )
				.property( "street1", anotherAddress.getStreet1() )
				.property( "postal_code", anotherAddress.getZipCode() );

		RelationshipsChainForGraphAssertions relationship1 = accountNode.relationshipTo( addressNode, "addresses" );
		RelationshipsChainForGraphAssertions relationship2 = accountNode.relationshipTo( anotherNode, "addresses" );

		assertThatOnlyTheseNodesExist( accountNode, addressNode, anotherNode );
		assertThatOnlyTheseRelationshipsExist( relationship1, relationship2 );
	}

	@Test
	public void testNoNodeIsLeftBehindWhenDeletingRelationships() throws Exception {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		MultiAddressAccount multiAddressAccount = em.find( MultiAddressAccount.class, account.getLogin() );
		multiAddressAccount.getAddresses().clear();
		em.getTransaction().commit();
		em.close();

		NodeForGraphAssertions accountNode = node( "account", MultiAddressAccount.class.getSimpleName(), ENTITY.name() )
				.property( "login", account.getLogin() )
				.property( "password", account.getPassword() );

		assertThatOnlyTheseNodesExist( accountNode );
		assertNumberOfRelationships( 0 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { MultiAddressAccount.class, Address.class };
	}

}
