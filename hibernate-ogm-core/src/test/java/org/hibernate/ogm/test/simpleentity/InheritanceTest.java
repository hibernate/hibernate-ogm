/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.simpleentity;

import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * @author Emmanuel Bernard
 */
public class InheritanceTest extends OgmTestCase {

	public void testInheritance() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();
		Hero h = new Hero();
		h.setName( "Spartacus" );
		session.persist( h );
		SuperHero sh = new SuperHero();
		sh.setName( "Batman" );
		sh.setSpecialPower( "Technology and samurai techniques" );
		session.persist( sh );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		Hero lh = (Hero) session.get( Hero.class, h.getName() );
		assertNotNull( lh );
		assertEquals( h.getName(), lh.getName() );
		SuperHero lsh = (SuperHero) session.get( SuperHero.class, sh.getName() );
		assertNotNull( lsh );
		assertEquals( sh.getSpecialPower(), lsh.getSpecialPower() );
		session.delete( lh );
		session.delete( lsh );
		transaction.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Hero.class,
				SuperHero.class
		};
	}
}
