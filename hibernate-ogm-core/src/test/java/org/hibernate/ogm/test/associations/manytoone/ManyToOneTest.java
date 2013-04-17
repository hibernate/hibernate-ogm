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
package org.hibernate.ogm.test.associations.manytoone;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.assertNumberOfAssociations;
import static org.hibernate.ogm.test.utils.TestHelper.assertNumberOfEntities;
import static org.hibernate.ogm.test.utils.TestHelper.get;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;

/**
 * @author Emmanuel Bernard
 */
public class ManyToOneTest extends OgmTestCase {

	public void testUnidirectionalManyToOne() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		JUG jug = new JUG();
		jug.setName( "JUG Summer Camp" );
		session.persist( jug );
		Member emmanuel = new Member();
		emmanuel.setName( "Emmanuel Bernard" );
		emmanuel.setMemberOf( jug );
		Member jerome = new Member();
		jerome.setName( "Jerome" );
		jerome.setMemberOf( jug );
		session.persist( emmanuel );
		session.persist( jerome );
		session.flush();
		assertThat( assertNumberOfEntities( 3, sessions ) ).isTrue();
		assertThat( assertNumberOfAssociations( 0, sessions ) ).isTrue();
		transaction.commit();
		assertThat( assertNumberOfEntities( 3, sessions ) ).isTrue();
		assertThat( assertNumberOfAssociations( 0, sessions ) ).isTrue();

		session.clear();

		transaction = session.beginTransaction();
		emmanuel = (Member) session.get( Member.class, emmanuel.getId() );
		jug = emmanuel.getMemberOf();
		session.delete( emmanuel );
		jerome = (Member) session.get( Member.class, jerome.getId() );
		session.delete( jerome );
		session.delete( jug );
		transaction.commit();
		assertThat( assertNumberOfEntities( 0, sessions ) ).isTrue();
		assertThat( assertNumberOfAssociations( 0, sessions ) ).isTrue();

		session.close();

		checkCleanCache();
	}

	public void testBidirectionalManyToOneRegular() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		SalesForce force = new SalesForce();
		force.setCorporation( "Red Hat" );
		session.save( force );
		SalesGuy eric = new SalesGuy();
		eric.setName( "Eric" );
		eric.setSalesForce( force );
		force.getSalesGuys().add( eric );
		session.save( eric );
		SalesGuy simon = new SalesGuy();
		simon.setName( "Simon" );
		simon.setSalesForce( force );
		force.getSalesGuys().add( simon );
		session.save( simon );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		force = (SalesForce) session.get( SalesForce.class, force.getId() );
		assertNotNull( force.getSalesGuys() );
		assertEquals( 2, force.getSalesGuys().size() );
		simon = (SalesGuy) session.get( SalesGuy.class, simon.getId() );
		// purposely faulty
		// force.getSalesGuys().remove( simon );
		session.delete( simon );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		force = (SalesForce) session.get( SalesForce.class, force.getId() );
		assertNotNull( force.getSalesGuys() );
		assertEquals( 1, force.getSalesGuys().size() );
		session.delete( force.getSalesGuys().iterator().next() );
		session.delete( force );
		transaction.commit();

		session.close();

		checkCleanCache();
	}

	public void testBiDirManyToOneInsertUpdateFalse() throws Exception {
		final Session session = openSession();
		Transaction tx = session.beginTransaction();
		Beer hoegaarden = new Beer();
		Brewery hoeBrewery = new Brewery();
		hoeBrewery.getBeers().add( hoegaarden );
		hoegaarden.setBrewery( hoeBrewery );
		session.persist( hoeBrewery );
		tx.commit();
		session.clear();

		tx = session.beginTransaction();
		hoegaarden = get( session, Beer.class, hoegaarden.getId() );
		assertThat( hoegaarden ).isNotNull();
		assertThat( hoegaarden.getBrewery() ).isNotNull();
		assertThat( hoegaarden.getBrewery().getBeers() )
			.hasSize( 1 )
			.containsOnly( hoegaarden );
		Beer citron = new Beer();
		hoeBrewery = hoegaarden.getBrewery();
		hoeBrewery.getBeers().remove( hoegaarden );
		hoeBrewery.getBeers().add( citron );
		citron.setBrewery( hoeBrewery );
		session.delete( hoegaarden );
		tx.commit();

		session.clear();

		tx = session.beginTransaction();
		citron = get( session, Beer.class, citron.getId() );
		assertThat( citron.getBrewery().getBeers() )
			.hasSize( 1 )
			.containsOnly( citron );
		hoeBrewery = citron.getBrewery();
		citron.setBrewery( null );
		hoeBrewery.getBeers().clear();
		session.delete( citron );
		session.delete( hoeBrewery );
		tx.commit();

		session.close();

		checkCleanCache();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { JUG.class, Member.class, SalesForce.class, SalesGuy.class, Beer.class, Brewery.class };
	}
}
