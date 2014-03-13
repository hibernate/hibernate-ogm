/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.backendtck.embeddable;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Test;

/**
 * Tests for {@code @Embeddable} types and {@code @ElementCollection}s there-of.
 *
 * @author Emmanuel Bernard
 * @Gunnar Morling
 */
public class EmbeddableTest extends OgmTestCase {

	@Test
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
		address.setStreet1( "1 avenue des Champs Elysees" );
		address.setZipCode( "75007" );
		session.persist( account );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		final Account loadedAccount = (Account) session.get( Account.class, account.getLogin() );
		assertThat( loadedAccount ).as( "Cannot load persisted object" ).isNotNull();
		final Address loadedAddress = loadedAccount.getHomeAddress();
		assertThat( loadedAddress ).as( "Embeddable should not be null" ).isNotNull();
		assertThat( loadedAddress.getCity() ).as( "persist and load fails for embeddable" ).isEqualTo( address.getCity() );
		assertThat( loadedAddress.getZipCode() ).as( "@Column support for embeddable does not work" ).isEqualTo( address.getZipCode() );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		loadedAddress.setCountry( "USA" );
		session.merge( loadedAccount );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		Account secondLoadedAccount = (Account) session.get( Account.class, account.getLogin() );
		assertThat( loadedAccount.getHomeAddress().getCity() ).as( "Merge fails for embeddable" ).isEqualTo( secondLoadedAccount.getHomeAddress().getCity() );
		session.delete( secondLoadedAccount );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		assertThat( session.get( Account.class, account.getLogin() ) ).isNull();
		transaction.commit();

		session.close();
	}

	@Test
	public void testElementCollectionOfEmbeddable() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();

		Address address = new Address();
		address.setCity( "Paris" );
		address.setCountry( "France" );
		address.setStreet1( "1 avenue des Champs Elysees" );
		address.setZipCode( "75007" );

		Address anotherAddress = new Address();
		anotherAddress.setCity( "Rome" );
		anotherAddress.setCountry( "Italy" );
		anotherAddress.setStreet1( "Piazza del Colosseo, 1" );
		anotherAddress.setZipCode( "00184" );

		MultiAddressAccount account = new MultiAddressAccount();
		account.setLogin( "gunnar" );
		account.setPassword( "highly secret" );
		account.getAddresses().add( address );
		account.getAddresses().add( anotherAddress );

		session.persist( account );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		MultiAddressAccount loadedAccount = (MultiAddressAccount) session.get( MultiAddressAccount.class, account.getLogin() );
		assertThat( loadedAccount ).as( "Cannot load persisted object" ).isNotNull();
		assertThat( loadedAccount.getAddresses() ).onProperty( "city" ).contains( "Paris", "Rome" );
		assertThat( loadedAccount.getAddresses() ).onProperty( "zipCode" ).contains( "75007", "00184" );
		assertThat( loadedAccount.getAddresses() ).onProperty( "country" ).contains( "France", "Italy" );

		Address loadedAddress1 = loadedAccount.getAddresses().get( 0 );
		Address loadedAddress2 = loadedAccount.getAddresses().get( 1 );

		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		loadedAddress1.setCountry( "USA" );
		loadedAddress2.setCountry( "Germany" );

		session.merge( loadedAccount );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		MultiAddressAccount secondLoadedAccount = (MultiAddressAccount) session.get( MultiAddressAccount.class, account.getLogin() );
		assertThat( secondLoadedAccount.getAddresses() ).onProperty( "city" ).contains( "Paris", "Rome" );
		assertThat( secondLoadedAccount.getAddresses() ).onProperty( "country" ).contains( "USA", "Germany" );
		session.delete( secondLoadedAccount );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		assertThat( session.get( MultiAddressAccount.class, account.getLogin() ) ).isNull();
		transaction.commit();

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Account.class, MultiAddressAccount.class };
	}
}
