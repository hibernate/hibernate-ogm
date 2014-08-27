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
public class BidirectionalManyToManyTest extends Neo4jJpaTestCase {

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
		String accountNode = "(a:BankAccount:ENTITY {id: {a}.id, accountNumber: {a}.accountNumber })";

		assertExpectedMapping( "o", ownerNode, params( barclays, owner ) );
		assertExpectedMapping( "a", accountNode, params( barclays, owner ) );
		assertExpectedMapping( "a", accountNode, params( soge, owner ) );
		assertExpectedMapping( "r", ownerNode + " - [r:bankAccounts] - " + accountNode, params( barclays, owner ) );
		assertExpectedMapping( "r", ownerNode + " - [r:bankAccounts] - " + accountNode, params( soge, owner ) );
	}

	private Map<String, Object> params(BankAccount account, AccountOwner owner) {
		Map<String, Object> accountProperties = accountProperties( account );
		Map<String, Object> ownerProperties = ownerProperties( owner );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "o", ownerProperties );
		params.put( "a", accountProperties );
		return params;
	}

	private Map<String, Object> accountProperties(BankAccount account) {
		Map<String, Object> accountProperties = new HashMap<String, Object>();
		accountProperties.put( "id", account.getId() );
		accountProperties.put( "accountNumber", account.getAccountNumber() );
		return accountProperties;
	}

	private Map<String, Object> ownerProperties(AccountOwner owner) {
		Map<String, Object> ownerProperties = new HashMap<String, Object>();
		ownerProperties.put( "id", owner.getId() );
		ownerProperties.put( "SSN", owner.getSSN() );
		return ownerProperties;
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { AccountOwner.class, BankAccount.class };
	}

}
