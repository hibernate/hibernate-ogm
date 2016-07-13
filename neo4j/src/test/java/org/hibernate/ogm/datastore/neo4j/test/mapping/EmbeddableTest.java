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

import org.hibernate.ogm.backendtck.embeddable.Account;
import org.hibernate.ogm.backendtck.embeddable.Address;
import org.hibernate.ogm.backendtck.embeddable.AddressType;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class EmbeddableTest extends Neo4jJpaTestCase {

	private Account account;
	private Address address;

	@Before
	public void prepareDB() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		account = new Account();
		account.setLogin( "emmanuel" );
		account.setPassword( "like I would tell ya" );
		account.setHomeAddress( new Address() );

		address = account.getHomeAddress();
		address.setCity( "Paris" );
		address.setCountry( "France" );
		address.setStreet1( "1 avenue des Champs Elysees" );
		address.setZipCode( "75007" );
		address.setType( new AddressType( "HOME" ) );

		em.persist( account );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testEmbeddedNodesMapping() throws Exception {
		NodeForGraphAssertions accountNode = node( "account", Account.class.getSimpleName(), ENTITY.name() )
				.property( "login", account.getLogin() )
				.property( "password", account.getPassword() )
				.property( "version", account.getVersion() )
				.property( "postal_code", account.getHomeAddress().getZipCode() );

		NodeForGraphAssertions homeAddressNode = node( "home", EMBEDDED.name() )
				.property( "street1", address.getStreet1() )
				.property( "city", address.getCity() )
				.property( "country", address.getCountry() );

		NodeForGraphAssertions typeNode = node( "type", EMBEDDED.name() )
				.property( "name", address.getType().getName() );

		RelationshipsChainForGraphAssertions relationship = accountNode
				.relationshipTo( homeAddressNode, "homeAddress" ).relationshipTo( typeNode, "type" );

		assertThatOnlyTheseNodesExist( accountNode, homeAddressNode, typeNode );
		assertThatOnlyTheseRelationshipsExist( relationship );
	}

	@Test
	public void testRemovePropertyFromEmbeddedNode() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		Account found = em.find( Account.class, account.getLogin() );
		found.getHomeAddress().setCity( null );
		em.getTransaction().commit();
		em.close();

		NodeForGraphAssertions accountNode = node( "account", Account.class.getSimpleName(), ENTITY.name() )
				.property( "login", account.getLogin() )
				.property( "password", account.getPassword() )
				.property( "version", account.getVersion() + 1 )
				.property( "postal_code", account.getHomeAddress().getZipCode() );

		NodeForGraphAssertions homeAddressNode = node( "home", EMBEDDED.name() )
				.property( "street1", address.getStreet1() )
				.property( "country", address.getCountry() );

		NodeForGraphAssertions typeNode = node( "type", EMBEDDED.name() )
				.property( "name", address.getType().getName() );

		RelationshipsChainForGraphAssertions relationship = accountNode
				.relationshipTo( homeAddressNode, "homeAddress" ).relationshipTo( typeNode, "type" );

		assertThatOnlyTheseNodesExist( accountNode, homeAddressNode, typeNode );
		assertThatOnlyTheseRelationshipsExist( relationship );
	}

	@Test
	public void testRemoveEmbeddedWhenPropertyIsSetToNull() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		Account found = em.find( Account.class, account.getLogin() );
		found.getHomeAddress().setType( null );
		em.getTransaction().commit();
		em.close();

		NodeForGraphAssertions accountNode = node( "account", Account.class.getSimpleName(), ENTITY.name() )
				.property( "login", account.getLogin() )
				.property( "password", account.getPassword() )
				.property( "version", account.getVersion() + 1 )
				.property( "postal_code", account.getHomeAddress().getZipCode() );

		NodeForGraphAssertions homeAddressNode = node( "home", EMBEDDED.name() )
				.property( "street1", address.getStreet1() )
				.property( "city", address.getCity() )
				.property( "country", address.getCountry() );

		RelationshipsChainForGraphAssertions relationship = accountNode.relationshipTo( homeAddressNode, "homeAddress" );

		assertThatOnlyTheseNodesExist( accountNode, homeAddressNode );
		assertThatOnlyTheseRelationshipsExist( relationship );
	}

	@Test
	public void testRemoveEmbeddedWhenIntermediateEmbeddedIsRemoved() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		Account found = em.find( Account.class, account.getLogin() );
		found.setHomeAddress( null );
		em.getTransaction().commit();
		em.close();

		NodeForGraphAssertions accountNode = node( "account", Account.class.getSimpleName(), ENTITY.name() )
				.property( "login", account.getLogin() )
				.property( "password", account.getPassword() )
				.property( "version", account.getVersion() + 1 );

		assertThatOnlyTheseNodesExist( accountNode );
		assertNumberOfRelationships( 0 );
	}

	@Test
	public void testRemoveEmbeddedWhenOwnerIsRemoved() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		Account found = em.find( Account.class, account.getLogin() );
		em.remove( found );
		em.getTransaction().commit();
		em.close();

		assertNumberOfNodes( 0 );
		assertNumberOfRelationships( 0 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Account.class, Address.class };
	}
}
