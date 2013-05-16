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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.hql.ParsingException;
import org.hibernate.ogm.test.utils.SessionFactoryRule;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class SimpleQueriesTest {

	private static class Addresses {

		public static Address MAIN_STREET;

		static {
			MAIN_STREET = new Address();
			MAIN_STREET.setId( 1L );
			MAIN_STREET.setCity( "London" );
			MAIN_STREET.setStreet( "Main Street" );
		}
	}

	private static class Authors {

		public static Author ALFRED;

		static {
			ALFRED = new Author();
			ALFRED.setId( 1L );
			ALFRED.setName( "Alfred" );
			ALFRED.setAddress( Addresses.MAIN_STREET );
		}
	}

	private static class Hypotheses {

		public static Hypothesis STUFF_WORKS;

		static {
			Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone( "GMT" ) );
			calendar.clear();
			calendar.set( 2012, 8, 25 );

			calendar.set( Calendar.YEAR, 2009 );
			STUFF_WORKS = new Hypothesis();
			STUFF_WORKS.setId( "16" );
			STUFF_WORKS.setDescription( "stuff works" );
			STUFF_WORKS.setPosition( 4 );
			STUFF_WORKS.setDate( calendar.getTime() );
			STUFF_WORKS.setAuthor( Authors.ALFRED );
		}

		public static void persistAll(Session session) {
			session.persist( STUFF_WORKS );
		}
	}

	@ClassRule
	public static final SessionFactoryRule sessions = new SessionFactoryRule( Hypothesis.class, Author.class, Helicopter.class, Address.class );

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
//	@Ignore
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
//	@Ignore
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
//	@Ignore
	public void testConstantParameterQueries() throws Exception {
		final Session session = sessions.openSession();

		assertQuery( session, 1, session.createQuery(
				"from Hypothesis h where h.description = 'stuff works'" ) );
		session.close();
	}

	@Test
//	@Ignore
	public void testUnqualifiedQuery() throws Exception {
		final Session session = sessions.openSession();
		List<Object> expectedResult = new ArrayList<Object>();
		expectedResult.add( new Object[] { "16", "stuff works" } );

		assertQueryResult(
				session,
				session.createQuery( "select id, description from Hypothesis h where id = 16" ),
				expectedResult
		);
		session.close();
	}

	@Test
//	@Ignore
	public void testSimpleProjectionQuery() throws Exception {
		final Session session = sessions.openSession();
		List<Object> expectedResult = new ArrayList<Object>();
		expectedResult.add( new Object[] { "16", "stuff works" } );

		assertQueryResult(
				session,
				session.createQuery( "select h.id, h.description from Hypothesis h where h.id = 16" ),
				expectedResult
		);
		session.close();
	}

	@Test
	public void testSelectingAttributeFromEmbeddedEntityInProjectionQuery() throws Exception {
		final Session session = sessions.openSession();
		List<Object> expectedResult = new ArrayList<Object>();
		expectedResult.add( new Object[] { "Alfred" } );

		assertQueryResult(
				session,
				session.createQuery( "select h.author.name from Hypothesis h where h.id = 16" ),
				expectedResult
		);
		session.close();
	}

	@Test
	public void testSelectingAttributeFromNestedEmbeddedEntityInProjectionQuery() throws Exception {
		final Session session = sessions.openSession();
		List<Object> expectedResult = new ArrayList<Object>();
		expectedResult.add( new Object[] { "Main Street" } );

		assertQueryResult(
				session,
				session.createQuery( "select h.author.address.street from Hypothesis h where h.id = 16" ),
				expectedResult
		);
		session.close();
	}

	@Test
//	@Ignore
	public void testSelectingCompleteEntityInProjectionQuery() throws Exception {
		final Session session = sessions.openSession();
		List<Object> expectedResult = new ArrayList<Object>();
		expectedResult.add( new Object[] { Hypotheses.STUFF_WORKS, "16" } );

		assertQueryResult(
				session,
				session.createQuery( "select h, h.id from Hypothesis h where h.id = 16" ),
				expectedResult
		);
		session.close();
	}

	@Test
//	@Ignore
	public void testSelectingCompleteEmbeddedEntityInProjectionQueryRaisesException() throws Exception {
		final Session session = sessions.openSession();

		thrown.expect( ParsingException.class );
		thrown.expectMessage( "HQLLUCN000008" );
		assertQueryResult(
				session,
				session.createQuery( "select h.author from Hypothesis h" ),
				null
		);
		session.close();
	}

	@Test
	@Ignore
	public void testProjectionQuery() throws Exception {
		final Session session = sessions.openSession();

		FullTextSession fullTextSession = Search.getFullTextSession( session );

		org.apache.lucene.search.Query query = fullTextSession
			.getSearchFactory()
			.buildQueryBuilder()
			.forEntity( Hypothesis.class )
			.get()
				.keyword()
				.onField( "description" )
				.matching( "stuff works" )
			.createQuery();

//		session.createQuery( "from Hypothesis h where h.description = 'stuff works'" );

		Transaction transaction = session.beginTransaction();

		FullTextQuery fullTextQuery =  fullTextSession.createFullTextQuery( query, Hypothesis.class );
		fullTextQuery.setProjection( "id", "description", "author.name" );
		List<?> results = fullTextQuery.list();
		Object[] firstResult = (Object[]) results.get(0);
		String id = (String) firstResult[0];
		String description = (String) firstResult[1];
		String authorName = (String) firstResult[2];

		System.out.println("ID: " + id + ", description: " + description + ", author: " + authorName);
		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	@Ignore
	public void testProjectionQuery2() throws Exception {
		final Session session = sessions.openSession();

		FullTextSession fullTextSession = Search.getFullTextSession( session );

		org.apache.lucene.search.Query query = fullTextSession
			.getSearchFactory()
			.buildQueryBuilder()
			.forEntity( Hypothesis.class )
			.get()
				.keyword()
				.onField( "author.name" )
				.matching( "Alfred" )
			.createQuery();

//		session.createQuery( "from Hypothesis h where h.description = 'stuff works'" );

		Transaction transaction = session.beginTransaction();

		FullTextQuery fullTextQuery =  fullTextSession.createFullTextQuery( query, Hypothesis.class );
		fullTextQuery.setProjection( "id", "description", "author.name" );
		List<?> results = fullTextQuery.list();
		Object[] firstResult = (Object[]) results.get(0);
		String id = (String) firstResult[0];
		String description = (String) firstResult[1];
		String authorName = (String) firstResult[2];

		System.out.println("ID: " + id + ", description: " + description + ", author: " + authorName);
		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
	public void testProjectionQuery3() throws Exception {
		final Session session = sessions.openSession();

		FullTextSession fullTextSession = Search.getFullTextSession( session );

		org.apache.lucene.search.Query query = fullTextSession
			.getSearchFactory()
			.buildQueryBuilder()
			.forEntity( Hypothesis.class )
			.get()
				.keyword()
				.onField( "author.name" )
				.matching( "Alfred" )
			.createQuery();

//		session.createQuery( "from Hypothesis h where h.description = 'stuff works'" );

		Transaction transaction = session.beginTransaction();

		FullTextQuery fullTextQuery =  fullTextSession.createFullTextQuery( query, Hypothesis.class );
		fullTextQuery.setProjection( "author" );
		List<?> results = fullTextQuery.list();
		Object[] firstResult = (Object[]) results.get(0);
//		String id = (String) firstResult[0];
//		String description = (String) firstResult[1];
//		String authorName = (String) firstResult[2];

		System.out.println("Result: " + Arrays.toString( firstResult ) );
		transaction.commit();
		session.clear();
		session.close();
	}

	@Test
//	@Ignore
	public void testNegatedQuery() throws Exception {
		final Session session = sessions.openSession();

		assertQuery( session, 3, session.createQuery(
				"from Hypothesis h where not h.id = '13'" ) );
		session.close();
	}

	@Test
//	@Ignore
	public void testQueryWithConjunctionAndNegation() throws Exception {
		final Session session = sessions.openSession();

		assertQuery( session, 1, session.createQuery(
				"from Hypothesis h where h.position = 2 and not h.id = '13'" ) );
		session.close();
	}

	@Test
//	@Ignore
	public void testQueryWithRangeAndNegation() throws Exception {
		final Session session = sessions.openSession();

		assertQuery( session, 2, session.createQuery(
				"from Hypothesis h where h.position between 2 and 3 and not h.id = '13'" ) );
		session.close();
	}

	@Test
//	@Ignore
	public void testQueryWithEmbeddedPropertyInFromClause() throws Exception {
		final Session session = sessions.openSession();
		List<Object> expectedResult = new ArrayList<Object>();
		expectedResult.add( new Object[] { Hypotheses.STUFF_WORKS } );

		assertQueryResult(
				session,
				session.createQuery( "from Hypothesis h where h.author.name = 'Alfred'" ),
				Hypotheses.STUFF_WORKS
		);
		session.close();
	}

	@Test
//	@Ignore
	public void testConstantNumericQuery() throws Exception {
		final Session session = sessions.openSession();

		assertQuery( session, 1, session.createQuery( "from Hypothesis h where h.id = 13" ) );
		session.close();
	}

	@Test
//	@Ignore
	public void testParametricQueries() throws Exception {
		final Session session = sessions.openSession();

		Query query = session
				.createQuery( "from Hypothesis h where h.description = :myParam" )
				.setString( "myParam", "stuff works" );
		assertQuery( session, 1, query );
		session.close();
	}

	@Test
//	@Ignore
	public void testConstantParameterRangeQuery() throws Exception {
		final Session session = sessions.openSession();

		// "Hilbert's..." and "Peano's..."
		assertQuery( session, 2, session.createQuery( "from Hypothesis h where h.description BETWEEN 'H' and 'Q'" ) );
		session.close();
	}

	@Test
//	@Ignore
	public void tesRangeQueryWithParameters() throws Exception {
		final Session session = sessions.openSession();

		// "Hilbert's..." and "Peano's..."
		Query query = session
				.createQuery( "from Hypothesis h where h.description BETWEEN :start and :end" )
				.setString( "start", "Hilbers" )
				.setString( "end", "Peanq" );

		assertQuery( session, 2, query );
		session.close();
	}

	@Test
//	@Ignore
	public void tesDateRangeQueryWithParameters() throws Exception {
		final Session session = sessions.openSession();

		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone( "GMT" ) );
		calendar.clear();
		calendar.set( 2009, 0, 1 );

		Date start = calendar.getTime();

		calendar.set( 2010, 11, 31 );
		Date end = calendar.getTime();

		// sanne and shortOne
		Query query = session
				.createQuery( "from Hypothesis h where h.date BETWEEN :start and :end" )
				.setDate( "start", start )
				.setDate( "end", end );

		assertQuery( session, 2, query );
		session.close();
	}

	@Test
//	@Ignore
	public void testConstantParameterNumericRangeQuery() throws Exception {
		final Session session = sessions.openSession();

		// "Hilbert's..." and "Peano's..."
		assertQuery( session, 2, session.createQuery( "from Hypothesis h where h.position BETWEEN 1 and 2" ) );
		session.close();
	}

	@Test
//	@Ignore
	public void testLargerThanQuery() throws Exception {
		final Session session = sessions.openSession();

		// "Hilbert's..." and "Peano's..."
		assertQuery( session, 2, session.createQuery( "from Hypothesis h where h.position > 2" ) );
		session.close();
	}

	@BeforeClass
	public static void setUp() throws Exception {
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone( "GMT" ) );
		calendar.clear();
		calendar.set( 2012, 8, 25 );

		final Session session = sessions.openSession();

		Transaction transaction = session.beginTransaction();

		Hypothesis socrates = new Hypothesis();
		socrates.setId( "13" );
		socrates.setDescription( "There are more than two dimensions over the shadows we see out of the cave" );
		socrates.setPosition( 1 );
		socrates.setDate( calendar.getTime() );
		session.persist( socrates );

		calendar.set( Calendar.YEAR, 2011 );
		Hypothesis peano = new Hypothesis();
		peano.setId( "14" );
		peano.setDescription( "Peano's curve and then Hilbert's space filling curve proof the connection from mono-dimensional to bi-dimensional space" );
		peano.setPosition( 2 );
		peano.setDate( calendar.getTime() );
		session.persist( peano );

		calendar.set( Calendar.YEAR, 2010 );
		Hypothesis sanne = new Hypothesis();
		sanne.setId( "15" );
		sanne.setDescription( "Hilbert's proof of connection to 2 dimensions can be induced to reason on N dimensions" );
		sanne.setPosition( 3 );
		sanne.setDate( calendar.getTime() );
		session.persist( sanne );

		Hypotheses.persistAll( session );

		Helicopter helicopter = new Helicopter();
		helicopter.setName( "No creative clue " );
		session.persist( helicopter );

		transaction.commit();
		session.close();
	}

	private void assertQuery(final Session session, final int expectedSize, final Query testedQuery) {
		Transaction transaction = session.beginTransaction();
		List<?> list = testedQuery.list();
		try {
			assertThat( list ).as( "Query failed" ).hasSize( expectedSize );
			System.out.println( "Results: ");
			for ( Object object : list ) {
				if( object instanceof Object[] ) {
					System.out.println( Arrays.deepToString( (Object[]) object ) );
				}
				else {
					System.out.println( object );
				}
			}
		}
		finally {
			transaction.commit();
			session.clear();
		}
	}

	private void assertQueryResult(Session session, Query testedQuery, List<?> expectedResult) {
		Transaction transaction = session.beginTransaction();
		List<?> list = testedQuery.list();
		try {
			Iterator<?> actualIterator = list.iterator();
			Iterator<?> expectedIterator = expectedResult.iterator();

			while( actualIterator.hasNext() ) {
				if( !expectedIterator.hasNext() ) {
					throw new AssertionError( String.format( "There are less results than expected. Actual results: %s, expected results: %s.", list, expectedResult ) );
				}

				Object actual = actualIterator.next();
				Object expected = expectedIterator.next();

				if( actual instanceof Object[] ) {
					assertThat( (Object[])actual ).isEqualTo( (Object[]) expected );
				}
				else {
					assertThat( actual ).isEqualTo( expected );
				}
			}

			if( expectedIterator.hasNext() ) {
				throw new AssertionError( String.format( "There are more results than expected. Actual results: %s, expected results: %s.", list, expectedResult ) );
			}
		}
		finally {
			transaction.commit();
			session.clear();
		}
	}

	private void assertQueryResult(Session session, Query testedQuery, Object expected) {
		Transaction transaction = session.beginTransaction();
		try {
			Object actual = testedQuery.uniqueResult();
			assertThat( actual ).isEqualTo( expected );
		}
		finally {
			transaction.commit();
			session.clear();
		}
	}
}
