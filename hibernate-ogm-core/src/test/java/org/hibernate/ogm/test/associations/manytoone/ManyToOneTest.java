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
package org.hibernate.ogm.test.associations.manytoone;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.TestHelper.getAssociationCache;
import static org.hibernate.ogm.test.utils.TestHelper.getEntityCache;

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
		session.persist( emmanuel );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		emmanuel = (Member) session.get( Member.class, emmanuel.getId() );
		jug = emmanuel.getMemberOf();
		session.delete( emmanuel );
		session.delete( jug );
		transaction.commit();

		assertThat(getEntityCache( session )).as("Entity cache should be empty").hasSize( 0 );
		assertThat(getAssociationCache( session )).as("Association cache should be empty").hasSize( 0 );

		session.close();
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
		//purposely faulty
		//force.getSalesGuys().remove( simon );
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

		assertThat(getEntityCache( session )).as("Entity cache should be empty").hasSize( 0 );
		assertThat(getAssociationCache( session )).as("Association cache should be empty").hasSize( 0 );


		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				JUG.class,
				Member.class,
				SalesForce.class,
				SalesGuy.class
		};
	}
}
