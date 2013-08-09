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
import java.util.List;
import java.util.TimeZone;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.hql.ParsingException;
import org.hibernate.ogm.test.utils.OgmTestCase;
import org.hibernate.ogm.test.utils.TestSessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 * @author Gunnar Morling
 */
public class SimpleQueriesTest extends OgmTestCase {

	@TestSessionFactory
	public static SessionFactory sessions;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Session session;

	@Before
	public void createSession() {
		session = sessions.openSession();
	}

	@After
	public void closeSession() {
		if ( session != null ) {
			session.close();
		}
	}

	@Test
	public void testSimpleQueries() throws Exception {
		assertQuery( session, 4, session.createQuery(
				"from Hypothesis" ) );
		assertQuery( session, 4, session.createQuery(
				"from org.hibernate.ogm.test.queries.Hypothesis" ) );
		assertQuery( session, 1, session.createQuery(
				"from Helicopter" ) );
	}

	@Test
	@Ignore("Requires HSEARCH 4.4 (see HSEARCH-703)")
	public void testSimpleQueryOnUnindexedSuperType() throws Exception {
		assertQuery( session, 5, session.createQuery(
				"from java.lang.Object" ) );
	}

	@Test
	public void testFailingQuery() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000024" );
		assertQuery( session, 4, session.createQuery( "from Object" ) ); // Illegal query
	}

	@Test
	public void testConstantParameterQueries() throws Exception {
		assertQuery( session, 1, session.createQuery(
				"from Hypothesis h where h.description = 'stuff works'" ) );
	}

	@Test
	public void testUnqualifiedProjectionQuery() throws Exception {
		List<ProjectionResult> projectionResult = asProjectionResults( "select id, description from Hypothesis h where id = 16" );
		assertThat( projectionResult ).containsOnly( new ProjectionResult( "16", "stuff works" ) );
	}

	@Test
	public void testQualifiedProjectionQuery() throws Exception {
		List<ProjectionResult> projectionResult = asProjectionResults( "select h.id, h.description from Hypothesis h where h.id = 16" );
		assertThat( projectionResult ).containsOnly( new ProjectionResult( "16", "stuff works" ) );
	}

	@Test
	public void testSelectingAttributeFromEmbeddedEntityInProjectionQuery() throws Exception {
		List<ProjectionResult> projectionResult = asProjectionResults( "select h.author.name from Hypothesis h where h.id = 16" );
		assertThat( projectionResult ).containsOnly( new ProjectionResult( "alfred" ) );
	}

	@Test
	public void testSelectingAttributeFromNestedEmbeddedEntityInProjectionQuery() throws Exception {
		List<ProjectionResult> projectionResult = asProjectionResults( "select h.author.address.street from Hypothesis h where h.id = 16" );
		assertThat( projectionResult ).containsOnly( new ProjectionResult( "Main Street" ) );
	}

	@Test
	public void testSelectingCompleteEntityInProjectionQuery() throws Exception {
		List<?> projectionResult = session.createQuery( "select h, h.id from Hypothesis h where h.id = 16" ).list();
		assertThat( projectionResult ).hasSize( 1 );
		Object[] singleResult = (Object[]) projectionResult.get( 0 );
		assertThat( ( (Hypothesis) singleResult[0] ).getId() ).isEqualTo( "16" );
		assertThat( singleResult[1] ).isEqualTo( "16" );
	}

	@Test
	public void testSelectingCompleteEmbeddedEntityInProjectionQueryRaisesException() throws Exception {
		thrown.expect( ParsingException.class );
		thrown.expectMessage( "HQLLUCN000005" );

		session.createQuery( "select h.author from Hypothesis h" ).list();
	}

	@Test
	public void testNegatedQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where not h.id = '13'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "14", "15", "16" );
	}

	@Test
	public void testQueryWithConjunctionAndNegation() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.position = 2 and not h.id = '13'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "14" );
	}

	@Test
	public void testQueryWithRangeAndNegation() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.position between 2 and 3 and not h.id = '13'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "14", "15" );
	}

	@Test
	public void testQueryWithEmbeddedPropertyInWhereClause() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.author.name = 'alfred'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "16" );
	}

	@Test
	public void testConstantNumericQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.id = 13" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "13" );
	}

	@Test
	public void testParametricQueries() throws Exception {
		List<?> result = session
				.createQuery( "from Hypothesis h where h.description = :myParam" )
				.setString( "myParam", "stuff works" )
				.list();
		assertThat( result ).onProperty( "id" ).containsOnly( "16" );
	}

	@Test
	public void testConstantParameterRangeQuery() throws Exception {
		List<?> result = session
				.createQuery( "from Hypothesis h where h.description BETWEEN 'H' and 'Q'" )
				.list();
		assertThat( result ).onProperty( "id" ).containsOnly( "14", "15" );
	}

	@Test
	public void testRangeQueryWithParameters() throws Exception {
		List<?> result = session
				.createQuery( "from Hypothesis h where h.description BETWEEN :start and :end" )
				.setString( "start", "Hilbers" )
				.setString( "end", "Peanq" )
				.list();

		assertThat( result ).onProperty( "id" ).containsOnly( "14", "15" );
	}

	@Test
	public void testDateRangeQueryWithParameters() throws Exception {
		Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ) );
		calendar.clear();
		calendar.set( 2009, 0, 1 );

		Date start = calendar.getTime();

		calendar.set( 2010, 11, 31 );
		Date end = calendar.getTime();

		List<?> result = session
				.createQuery( "from Hypothesis h where h.date BETWEEN :start and :end" )
				.setDate( "start", start )
				.setDate( "end", end )
				.list();

		assertThat( result ).onProperty( "id" ).containsOnly( "15", "16" );
	}

	@Test
	public void testConstantParameterNumericRangeQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.position BETWEEN 1 and 2" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "13", "14" );
	}

	@BeforeClass
	public static void insertTestEntities() throws Exception {
		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		Address mainStreet = new Address();
		mainStreet.setId( 1L );
		mainStreet.setCity( "London" );
		mainStreet.setStreet( "Main Street" );
		session.persist( mainStreet );

		Author alfred = new Author();
		alfred.setId( 1L );
		alfred.setName( "alfred" );
		alfred.setAddress( mainStreet );
		session.persist( alfred );

		Calendar calendar = Calendar.getInstance( TimeZone.getTimeZone( "GMT" ) );
		calendar.clear();
		calendar.set( 2012, 8, 25 );

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

		calendar.set( Calendar.YEAR, 2009 );
		Hypothesis shortOne = new Hypothesis();
		shortOne.setId( "16" );
		shortOne.setDescription( "stuff works" );
		shortOne.setPosition( 4 );
		shortOne.setDate( calendar.getTime() );
		shortOne.setAuthor( alfred );
		session.persist( shortOne );

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
		}
		finally {
			transaction.commit();
			session.clear();
		}
	}

	private List<ProjectionResult> asProjectionResults(String projectionQuery) {
		List<?> results = session.createQuery( projectionQuery ).list();
		List<ProjectionResult> projectionResults = new ArrayList<ProjectionResult>();

		for ( Object result : results ) {
			if ( !( result instanceof Object[] ) ) {
				throw new IllegalArgumentException( "No projection result: " + result );
			}
			projectionResults.add( ProjectionResult.forArray( (Object[]) result ) );
		}

		return projectionResults;
	}

	private static class ProjectionResult {

		private Object[] elements;

		public ProjectionResult(Object... elements) {
			this.elements = elements;
		}

		public static ProjectionResult forArray(Object[] element) {
			ProjectionResult result = new ProjectionResult();
			result.elements = element;
			return result;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode( elements );
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			if ( obj == null ) {
				return false;
			}
			if ( getClass() != obj.getClass() ) {
				return false;
			}
			ProjectionResult other = (ProjectionResult) obj;
			if ( !Arrays.equals( elements, other.elements ) ) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return Arrays.deepToString( elements );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Hypothesis.class, Helicopter.class, Author.class, Address.class };
	}
}
