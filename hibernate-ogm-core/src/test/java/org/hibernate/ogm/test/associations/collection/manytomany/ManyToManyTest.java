/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.test.associations.collection.manytomany;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.assertNumberOfAssociations;
import static org.hibernate.ogm.test.utils.TestHelper.assertNumberOfEntities;
import static org.hibernate.ogm.test.utils.TestHelper.get;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ManyToManyTest extends OgmTestCase {
	public void testManyToMany() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		AccountOwner owner = new AccountOwner();
		owner.setSSN( "0123456" );
		BankAccount soge = new BankAccount();
		soge.setAccountNumber( "X2345000" );
		owner.getBankAccounts().add( soge );
		soge.getOwners().add( owner );
		session.persist( owner );
		tx.commit();

		assertThat( assertNumberOfEntities( 2, sessions ) ).isTrue();
		assertThat( assertNumberOfAssociations( 2, sessions ) ).isTrue();

		session.clear();

		// read from inverse side
		tx = session.beginTransaction();
		soge = get( session, BankAccount.class, soge.getId() );
		assertThat( soge.getOwners() ).hasSize( 1 );
		assertThat( soge.getOwners() ).onProperty( "id" ).contains( owner.getId() );
		tx.commit();

		session.clear();

		// read from non-inverse side and update data
		tx = session.beginTransaction();
		owner = get( session, AccountOwner.class, owner.getId() );
		assertThat( owner.getBankAccounts() ).hasSize( 1 );
		assertThat( owner.getBankAccounts() ).onProperty( "id" ).contains( soge.getId() );
		BankAccount barclays = new BankAccount();
		barclays.setAccountNumber( "ZZZ-009" );
		barclays.getOwners().add( owner );
		soge = owner.getBankAccounts().iterator().next();
		soge.getOwners().remove( owner );
		owner.getBankAccounts().add( barclays );
		owner.getBankAccounts().remove( soge );
		session.delete( soge );
		tx.commit();

		assertThat( assertNumberOfEntities( 2, sessions ) ).isTrue();
		assertThat( assertNumberOfAssociations( 2, sessions ) ).isTrue();
		session.clear();

		// delete data
		tx = session.beginTransaction();
		owner = get( session, AccountOwner.class, owner.getId() );
		assertThat( owner.getBankAccounts() ).hasSize( 1 );
		assertThat( owner.getBankAccounts() ).onProperty( "id" ).contains( barclays.getId() );
		barclays = owner.getBankAccounts().iterator().next();
		barclays.getOwners().clear();
		owner.getBankAccounts().clear();
		session.delete( barclays );
		session.delete( owner );
		tx.commit();

		assertThat( assertNumberOfEntities( 0, sessions ) ).isTrue();
		assertThat( assertNumberOfAssociations( 0, sessions ) ).isTrue();

		session.close();
		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { AccountOwner.class, BankAccount.class };
	}
}
