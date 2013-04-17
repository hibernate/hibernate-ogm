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
package org.hibernate.ogm.test.associations.collection.types;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class MapTest extends OgmTestCase {

	public void testMapAndElementCollection() throws Exception {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		Address home = new Address();
		home.setCity( "Paris" );
		Address work = new Address();
		work.setCity( "San Francisco" );
		User user = new User();
		user.getAddresses().put( "home", home );
		user.getAddresses().put( "work", work );
		user.getNicknames().add( "idrA" );
		user.getNicknames().add( "day[9]" );
		session.persist( home );
		session.persist( work );
		session.persist( user );
		User user2 = new User();
		user2.getNicknames().add( "idrA" );
		user2.getNicknames().add( "day[9]" );
		session.persist( user2 );
		tx.commit();

		session.clear();

		tx = session.beginTransaction();
		user = (User) session.get( User.class, user.getId() );
		assertThat( user.getNicknames() ).as( "Should have 2 nick1" ).hasSize( 2 );
		assertThat( user.getNicknames() ).as( "Should contain nicks" ).contains( "idrA", "day[9]" );
		user.getNicknames().remove( "idrA" );
		tx.commit();

		session.clear();

		tx = session.beginTransaction();
		user = (User) session.get( User.class, user.getId() );
		// TODO do null value
		assertThat( user.getAddresses() ).as( "List should have 2 elements" ).hasSize( 2 );
		assertThat( user.getAddresses().get( "home" ).getCity() ).as( "home address should be under home" ).isEqualTo(
				home.getCity() );
		assertThat( user.getNicknames() ).as( "Should have 1 nick1" ).hasSize( 1 );
		assertThat( user.getNicknames() ).as( "Should contain nick" ).contains( "day[9]" );
		session.delete( user );
		session.delete( session.load( Address.class, home.getId() ) );
		session.delete( session.load( Address.class, work.getId() ) );

		user2 = (User) session.get( User.class, user2.getId() );
		assertThat( user2.getNicknames() ).as( "Should have 2 nicks" ).hasSize( 2 );
		assertThat( user2.getNicknames() ).as( "Should contain nick" ).contains( "idrA", "day[9]" );
		session.delete( user2 );

		tx.commit();

		session.close();

		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { User.class, Address.class };
	}
}
