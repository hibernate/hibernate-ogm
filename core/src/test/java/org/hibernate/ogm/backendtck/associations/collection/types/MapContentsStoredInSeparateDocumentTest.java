/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.backendtck.associations.collection.types;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfAssociations;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfEmbeddedCollections;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.junit.Test;

/**
 * Tests that map contents are stored in a separate association document if configured so, while the contents of
 * embedded collections should always be stored within the entity document.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 * @author Gunnar Morling
 */
@SkipByGridDialect(
		value = { GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.NEO4J },
		comment = "Only the document stores CouchDB and MongoDB support the configuration of specific association storage strategies"
)
public class MapContentsStoredInSeparateDocumentTest extends OgmTestCase {

	@Test
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

		assertThat( getNumberOfAssociations( sessions, AssociationStorageType.IN_ENTITY ) )
				.describedAs( "Map contents should be stored in separate document" )
				.isEqualTo( 0 );
		assertThat( getNumberOfAssociations( sessions, AssociationStorageType.ASSOCIATION_DOCUMENT ) )
				.describedAs( "Map contents should be stored in association document" )
				.isEqualTo( 1 );
		assertThat( getNumberOfEmbeddedCollections( sessions ) )
				.describedAs( "Element collection contents should be stored within the entity document" )
				.isEqualTo( 2 );

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

	@Override
	protected void configure(Configuration cfg) {
		cfg.getProperties().put( DocumentStoreProperties.ASSOCIATIONS_STORE, AssociationStorageType.ASSOCIATION_DOCUMENT );
	}
}
