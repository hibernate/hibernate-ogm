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
 * @author Nicolas Helleringer
 */
public class CRUDTest extends OgmTestCase {

	public void testSimpleCRUD() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();
		Hypothesis hyp = new Hypothesis();
		hyp.setId( "1234567890" );
		hyp.setDescription( "NP != P" );
		hyp.setPosition( 1 );
		session.persist( hyp );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		final Hypothesis loadedHyp = (Hypothesis) session.get( Hypothesis.class, hyp.getId() );
		assertNotNull( "Cannot load persisted object", loadedHyp );
		assertEquals( "persist and load fails", hyp.getDescription(), loadedHyp.getDescription() );
		assertEquals( "@Column fails", hyp.getPosition(), loadedHyp.getPosition() );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		loadedHyp.setDescription( "P != NP");
		session.merge( loadedHyp );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		Hypothesis secondLoadedHyp = (Hypothesis) session.get( Hypothesis.class, hyp.getId() );
		assertEquals( "Merge fails", loadedHyp.getDescription(), secondLoadedHyp.getDescription() );
		session.delete( secondLoadedHyp );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		assertNull( session.get( Hypothesis.class, hyp.getId() ) );
		transaction.commit();

		session.close();
	}

	private void performanceLoop() throws Exception {
		long start = 0;
		for ( int i = 0; i < Integer.MAX_VALUE; i++ ) {
			if ( i % 10000 == 0 ) {
				start = System.nanoTime();
			}
			testSimpleCRUD();
			if ( i % 10000 == 9999 ) {
				long elapsed = System.nanoTime() - start;
				System.out.printf( "%.3E ms for 10000 tests\n", (elapsed) / 1000000f );
			}
		}
	}

	public static void main(String[] args) throws Exception {
		CRUDTest test = new CRUDTest();
		test.setUp();
		try {
			test.performanceLoop();
		}
		finally {
			test.tearDown();
		}
	}

	public void testGeneratedValue() throws Exception {
		final Session session = openSession();

		Transaction transaction = session.beginTransaction();
		Helicopter h = new Helicopter();
		h.setName( "Eurocopter" );
		session.persist( h );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		h = (Helicopter) session.get( Helicopter.class, h.getUUID() );
		session.delete( h );
		transaction.commit();

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Hypothesis.class,
				Helicopter.class
		};
	}
}
