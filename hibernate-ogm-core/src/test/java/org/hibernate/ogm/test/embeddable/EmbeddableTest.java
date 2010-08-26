/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.test.embeddable;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;

/**
 * @author Emmanuel Bernard
 */
public class EmbeddableTest extends OgmTestCase {

	public void testEmbeddable() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();
		Account account = new Account();
		account.setLogin( "emmanuel" );
		account.setPassword( "like I would tell ya" );
		account.setHomeAddress( new Address() );
		final Address address = account.getHomeAddress();
		address.setCity( "Paris" );
		address.setCountry( "France" );
		address.setStreet1( "1 avenue des Champs Elysées" );
		address.setZipCode( "705007" );
		session.persist( account );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		final Account loadedAccount = (Account) session.get( Account.class, account.getLogin() );
		assertNotNull( "Cannot load persisted object", loadedAccount );
		final Address loadedAddress = loadedAccount.getHomeAddress();
		assertNotNull( "Embeddable should not be null", loadedAddress );
		assertEquals( "persist and load fails for embeddable", loadedAddress.getCity(), address.getCity() );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		loadedAddress.setCountry( "USA" );
		session.merge( loadedAccount );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		Account secondLoadedAccount = (Account) session.get( Account.class, account.getLogin() );
		assertEquals(
				"Merge fails for embeddable",
				loadedAccount.getHomeAddress().getCity(),
				secondLoadedAccount.getHomeAddress().getCity() );
		session.delete( secondLoadedAccount );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		assertNull( session.get( Account.class, account.getLogin() ) );
		transaction.commit();

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Account.class
		};
	}
}
