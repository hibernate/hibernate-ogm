/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.queries;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.test.utils.SessionFactoryRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class SimpleQueriesTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@ClassRule
	public static final SessionFactoryRule sessions = new SessionFactoryRule(
			Hypothesis.class,
			Helicopter.class
		);

	@Test
	public void testSimpleQueries() throws Exception {
		final Session session = sessions.openSession();

		assertQuery( session, 4, session.createQuery(
				"from Hypothesis" ) );
		assertQuery( session, 4, session.createQuery(
				"from org.hibernate.ogm.test.queries.Hypothesis" ) );
		assertQuery( session, 1, session.createQuery(
				"from Helicopter" ) );
		assertQuery( session, 5, session.createQuery(
				"from java.lang.Object" ) );
	}

	@Test
	public void testFailingQuery() {
		final Session session = sessions.openSession();
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000024" );
		try {
			assertQuery( session, 4, session.createQuery(
					"from Object" ) ); //Illegal query
		}
		finally {
			session.close();
		}
	}

	@Test
	public void testConstantParameterQueries() throws Exception {
		final Session session = sessions.openSession();

		assertQuery( session, 1, session.createQuery(
				"from Hypothesis h where h.description = 'stuff works'" ) );
		session.close();
	}

	@Test
	public void testParametricQueries() throws Exception {
		final Session session = sessions.openSession();

		Query query = session
				.createQuery( "from Hypothesis h where h.description = :myParam" )
				.setString( "myParam", "stuff works" );
		assertQuery( session, 1, query );
		session.close();
	}

	private void assertQuery(final Session session, final int expectedSize, final Query testedQuery) {
		Transaction transaction = session.beginTransaction();
		List list = testedQuery.list();
		try {
			assertThat( list ).as( "Query failed" ).hasSize( expectedSize );
		}
		finally {
			transaction.commit();
			session.clear();
		}
	}

	@BeforeClass
	public static void setUp() throws Exception {
		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		Hypothesis socrates = new Hypothesis();
		socrates.setId( "13" );
		socrates.setDescription( "There are more than two dimensions over the shadows we see out of the cave" );
		socrates.setPosition( 1 );
		session.persist( socrates );

		Hypothesis peano = new Hypothesis();
		peano.setId( "14" );
		peano.setDescription( "Peano's curve and then Hilbert's space filling curve proof the connection from mono-dimensional to bi-dimensional space" );
		peano.setPosition( 2 );
		session.persist( peano );

		Hypothesis sanne = new Hypothesis();
		sanne.setId( "15" );
		sanne.setDescription( "Hilbert's proof of connection to 2 dimensions can be induced to reason on N dimensions" );
		sanne.setPosition( 3 );
		session.persist( sanne );

		Hypothesis shortOne = new Hypothesis();
		shortOne.setId( "16" );
		shortOne.setDescription( "stuff works" );
		shortOne.setPosition( 4 );
		session.persist( shortOne );

		Helicopter helicopter = new Helicopter();
		helicopter.setName( "No creative clue " );
		session.persist( helicopter );

		transaction.commit();
		session.close();
	}

}
