/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

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

		owner = new AccountOwner();
		owner.setSSN( "0123456" );

		soge = new BankAccount();
		soge.setAccountNumber( "X2345000" );
		soge.getOwners().add( owner );
		owner.getBankAccounts().add( soge );

		barclays = new BankAccount();
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
		assertRelationships( 4 );

		String ownerNode = "(owner:AccountOwner:ENTITY {id: '" + owner.getId() + "', SSN: '" + owner.getSSN() + "' })";
		String barcklaysNode = "(barclays:BankAccount:ENTITY {id: '" + barclays.getId() + "', accountNumber: '" + barclays.getAccountNumber() + "' })";
		String sogeNode = "(soge:BankAccount:ENTITY {id: '" + soge.getId() + "', accountNumber: '" + soge.getAccountNumber() + "' })";

		// Bidirectional relationship with Barcklays
		assertExpectedMapping( ownerNode + "- [:bankAccounts] -> " + barcklaysNode );
		assertExpectedMapping( barcklaysNode + " - [:owners] -> " + ownerNode );

		// Bidirectional relationship with Sorge
		assertExpectedMapping( ownerNode + " - [:bankAccounts] -> " + sogeNode );
		assertExpectedMapping( sogeNode + " - [:owners] -> " + ownerNode );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { AccountOwner.class, BankAccount.class };
	}

}
