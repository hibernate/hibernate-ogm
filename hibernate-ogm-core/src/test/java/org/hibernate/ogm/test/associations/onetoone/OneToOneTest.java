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
package org.hibernate.ogm.test.associations.onetoone;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.utils.OgmTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class OneToOneTest extends OgmTestCase {

	@Test
	public void testUnidirectionalManyToOne() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Horse horse = new Horse();
		horse.setName( "Palefrenier" );
		Cavalier cavalier = new Cavalier();
		cavalier.setName( "Caroline" );
		cavalier.setHorse( horse );
		session.persist( horse );
		session.persist( cavalier );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		cavalier = (Cavalier) session.get( Cavalier.class, cavalier.getId() );
		horse = cavalier.getHorse();
		session.delete( cavalier );
		session.delete( horse );
		transaction.commit();

		session.close();

		checkCleanCache();
	}

	@Test
	public void testUnidirectionalOneToOne() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Vehicule vehicule = new Vehicule();
		vehicule.setBrand( "Mercedes" );
		Wheel wheel = new Wheel();
		wheel.setVehicule( vehicule );
		session.persist( vehicule );
		session.persist( wheel );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		wheel = (Wheel) session.get( Wheel.class, wheel.getId() );
		vehicule = wheel.getVehicule();
		session.delete( wheel );
		session.delete( vehicule );
		transaction.commit();
		session.close();
	}

	@Test
	public void testBidirectionalManyToOne() throws Exception {
		final Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Husband husband = new Husband();
		husband.setName( "Alex" );
		Wife wife = new Wife();
		wife.setName( "Bea" );
		husband.setWife( wife );
		wife.setHusband( husband );
		session.persist( husband );
		session.persist( wife );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		husband = (Husband) session.get( Husband.class, husband.getId() );
		assertNotNull( husband );
		assertNotNull( husband.getWife() );
		session.clear();
		wife = (Wife) session.get( Wife.class, wife.getId() );
		assertNotNull( wife );
		husband = wife.getHusband();
		assertNotNull( husband );
		Wife bea2 = new Wife();
		session.persist( bea2 );
		bea2.setName( "Still Bea" );
		husband.setWife( bea2 );
		wife.setHusband( null );
		bea2.setHusband( husband );
		transaction.commit();
		session.clear();

		transaction = session.beginTransaction();
		husband = (Husband) session.get( Husband.class, husband.getId() );
		assertNotNull( husband );
		assertNotNull( husband.getWife() );
		session.clear();
		wife = (Wife) session.get( Wife.class, wife.getId() );
		assertNotNull( wife );
		assertNull( wife.getHusband() );
		session.delete( wife );
		bea2 = (Wife) session.get( Wife.class, bea2.getId() );
		assertNotNull( bea2 );
		husband = bea2.getHusband();
		assertNotNull( husband );
		bea2.setHusband( null );
		husband.setWife( null );
		session.delete( husband );
		session.delete( wife );
		session.delete( bea2 );
		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Horse.class,
				Cavalier.class,
				Vehicule.class,
				Wheel.class,
				Husband.class,
				Wife.class
		};
	}
}
