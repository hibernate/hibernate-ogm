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
package org.hibernate.ogm.test.associations.onetoone;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.simpleentity.OgmTestCase;

/**
 * @author Emmanuel Bernard
 */
public class OneToOneTest extends OgmTestCase {
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
	}

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

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Horse.class,
				Cavalier.class,
				Vehicule.class,
				Wheel.class
		};
	}
}
