/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.collection.manytomany.AccountOwner;
import org.hibernate.ogm.backendtck.associations.collection.manytomany.BankAccount;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class BidirectionalManyToManyTest extends Neo4jJpaTestCase {

	private AccountOwner owner;
	private BankAccount soge;
	private BankAccount barclays;

	@Before
	public void prepareDb() throws Exception {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		owner = new AccountOwner( "owner_1" );
		owner.setSSN( "0123456" );

		soge = new BankAccount( "account_1" );
		soge.setAccountNumber( "X2345000" );
		soge.getOwners().add( owner );
		owner.getBankAccounts().add( soge );

		barclays = new BankAccount( "account_2" );
		barclays.setAccountNumber( "ZZZ-009" );
		barclays.getOwners().add( owner );
		owner.getBankAccounts().add( barclays );

		em.persist( owner );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions ownerNode = node( "o", AccountOwner.class.getSimpleName(), ENTITY.name() )
				.property( "id", owner.getId() )
				.property( "SSN", owner.getSSN() );

		NodeForGraphAssertions barclaysNode = node( "b", BankAccount.class.getSimpleName(), ENTITY.name() )
				.property( "id", barclays.getId() )
				.property( "accountNumber", barclays.getAccountNumber() );

		NodeForGraphAssertions sogeNode = node( "s", BankAccount.class.getSimpleName(), ENTITY.name() )
				.property( "id", soge.getId() )
				.property( "accountNumber", soge.getAccountNumber() );

		RelationshipsChainForGraphAssertions relationship1 = ownerNode.relationshipTo( barclaysNode, "bankAccounts" );
		RelationshipsChainForGraphAssertions relationship2 = ownerNode.relationshipTo( sogeNode, "bankAccounts" );

		assertThatOnlyTheseNodesExist( ownerNode, barclaysNode, sogeNode );
		assertThatOnlyTheseRelationshipsExist( relationship1, relationship2 );

	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { AccountOwner.class, BankAccount.class };
	}

}
