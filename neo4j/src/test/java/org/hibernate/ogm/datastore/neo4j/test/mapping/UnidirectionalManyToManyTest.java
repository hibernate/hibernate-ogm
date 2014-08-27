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

import org.hibernate.ogm.backendtck.associations.collection.manytomany.AccountOwner;
import org.hibernate.ogm.backendtck.associations.collection.manytomany.BankAccount;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class UnidirectionalManyToManyTest extends Neo4jJpaTestCase {

	private AccountOwner owner;
	private BankAccount soge;
	private BankAccount barclays;

	@Before
	public void prepareDb() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

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
		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 3 );
		assertRelationships( 2 );

		String ownerNode = "(o:AccountOwner:ENTITY {id: {o}.id, SSN: {o}.SSN })";
		String barcklaysNode = "(b:BankAccount:ENTITY {id: {b}.id, accountNumber: {b}.accountNumber })";
		String sogeNode = "(s:BankAccount:ENTITY {id: {s}.id, accountNumber: {s}.accountNumber })";

		Map<String, Object> ownerProperties = new HashMap<String, Object>();
		ownerProperties.put( "id", owner.getId() );
		ownerProperties.put( "SSN", owner.getSSN() );

		Map<String, Object> barcklaysProperties = new HashMap<String, Object>();
		barcklaysProperties.put( "id", barclays.getId() );
		barcklaysProperties.put( "accountNumber", barclays.getAccountNumber() );

		Map<String, Object> sogeProperties = new HashMap<String, Object>();
		sogeProperties.put( "id", soge.getId() );
		sogeProperties.put( "accountNumber", soge.getAccountNumber() );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "o", ownerProperties );
		params.put( "b", barcklaysProperties );
		params.put( "s", sogeProperties );

		assertExpectedMapping( "o", ownerNode, params );
		assertExpectedMapping( "b", barcklaysNode, params );
		assertExpectedMapping( "s", sogeNode, params );
		assertExpectedMapping( "r", ownerNode + " - [r:bankAccounts] - " + barcklaysNode, params );
		assertExpectedMapping( "r", ownerNode + " - [r:bankAccounts] - " + sogeNode, params );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { AccountOwner.class, BankAccount.class };
	}

}
