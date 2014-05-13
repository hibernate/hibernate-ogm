/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012-2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.backendtck.queries;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.COUCHDB;
import static org.hibernate.ogm.utils.GridDialectType.EHCACHE;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.MONGODB;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J;

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
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestSessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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

	private Transaction tx;

	@Before
	public void createSession() {
		closeSession();
		session = sessions.openSession();
		tx = session.beginTransaction();
	}

	@After
	public void closeSession() {
		if ( tx != null && tx.isActive() ) {
			tx.commit();
			tx = null;
		}
		if ( session != null ) {
			session.close();
			session = null;
		}
	}

	@Test
	public void testSimpleQueries() throws Exception {
		assertQuery( session, 8, session.createQuery(
				"from Hypothesis" ) );
		assertQuery( session, 8, session.createQuery(
				"from org.hibernate.ogm.backendtck.queries.Hypothesis" ) );
		assertQuery( session, 3, session.createQuery(
				"from Helicopter" ) );
	}

	@Test
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Querying on supertypes is not yet implemented.")
	public void testSimpleQueryOnUnindexedSuperType() throws Exception {
		assertQuery( session, 11, session.createQuery(
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
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Selecting from embedded entities is not yet implemented.")
	public void testSelectingAttributeFromEmbeddedEntityInProjectionQuery() throws Exception {
		List<ProjectionResult> projectionResult = asProjectionResults( "select h.author.name from Hypothesis h where h.id = 16" );
		assertThat( projectionResult ).containsOnly( new ProjectionResult( "alfred" ) );
	}

	@Test
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Selecting from embedded entities is not yet implemented.")
	public void testSelectingAttributeFromNestedEmbeddedEntityInProjectionQuery() throws Exception {
		List<ProjectionResult> projectionResult = asProjectionResults( "select h.author.address.street from Hypothesis h where h.id = 16" );
		assertThat( projectionResult ).containsOnly( new ProjectionResult( "Main Street" ) );
	}

	@Test
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Projecting complete entity is not yet implemented.")
	public void testSelectingCompleteEntityInProjectionQuery() throws Exception {
		List<?> projectionResult = session.createQuery( "select h, h.id from Hypothesis h where h.id = 16" ).list();
		assertThat( projectionResult ).hasSize( 1 );
		Object[] singleResult = (Object[]) projectionResult.get( 0 );
		assertThat( ( (Hypothesis) singleResult[0] ).getId() ).isEqualTo( "16" );
		assertThat( singleResult[1] ).isEqualTo( "16" );
	}

	@Test
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Doesn't apply to MongoDB or Neo4j queries.")
	public void testSelectingCompleteEmbeddedEntityInProjectionQueryRaisesException() throws Exception {
		thrown.expect( ParsingException.class );
		thrown.expectMessage( "HQLLUCN000005" );

		session.createQuery( "select h.author from Hypothesis h" ).list();
	}

	@Test
	public void testRestrictingOnPropertyWithConfiguredColumnName() throws Exception {
		List<?> results = session.createQuery( "from Hypothesis h where h.position = '2'" ).list();
		assertThat( results ).onProperty( "id" ).containsOnly( "14" );
	}

	@Test
	public void testNegatedQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where not h.id = '13'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "14", "15", "16", "17", "18", "19", "20" );
	}

	@Test
	public void testNegatedQueryOnNumericProperty() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.position <> 4" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "13", "14", "15", "17", "18", "19" );
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
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Selecting from embedded entities is not yet implemented.")
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
		assertThat( result ).onProperty( "id" ).containsOnly( "14", "15", "17" );
	}

	@Test
	public void testRangeQueryWithParameters() throws Exception {
		List<?> result = session
				.createQuery( "from Hypothesis h where h.description BETWEEN :start and :end" )
				.setString( "start", "Hilbers" )
				.setString( "end", "Peanq" )
				.list();

		assertThat( result ).onProperty( "id" ).containsOnly( "14", "15", "17" );
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

	@Test
	public void testLessQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.position < 3" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "13", "14" );
	}

	@Test
	public void testNotLessQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where NOT h.position < 3" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "15", "16", "17", "18", "19", "20" );
	}

	@Test
	public void testLessOrEqualsQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.position <= 3" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "13", "14", "15" );
	}

	@Test
	public void testNotLessOrEqualsQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where NOT h.position <= 3" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "16", "17", "18", "19", "20" );
	}

	@Test
	public void testGreaterOrEqualsQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.position >= 2" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "14", "15", "16", "17", "18", "19", "20" );
	}

	@Test
	public void testGreaterQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.position > 2" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "15", "16", "17", "18", "19", "20" );
	}

	@Test
	public void testInQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.position IN (2, 3, 4)" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "14", "15", "16", "20" );
	}

	@Test
	public void testInQueryOnStringProperty() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.id IN ('15', '16')" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "15", "16" );
	}

	@Test
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Selecting from embedded entities is not yet implemented.")
	public void testInQueryOnEmbeddedEntity() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.author.name IN ('alma', 'alfred')" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "14", "16" );
	}

	@Test
	public void testNotInQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.position NOT IN (3, 4)" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "13", "14", "17", "18", "19" );
	}

	@Test
	public void testNotInQueryReturnsEntityWithQueriedPropertySetToNull() throws Exception {
		List<?> result = session.createQuery( "from Helicopter h where h.name NOT IN ('No creative clue')" ).list();
		assertThat( result ).onProperty( "name" ).containsOnly( null, "Lama" );
	}

	@Test
	public void testLikeQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.description LIKE '%dimensions%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "13", "15" );
	}

	@Test
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Querying on embedded entities is not yet implemented.")
	public void testLikeQueryWithSingleCharacterWildCard() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.author.name LIKE 'al_red'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "16" );
	}

	@Test
	public void testLikeQueryOnMultiwords() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.description LIKE 'There are more than%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "13", "20" );
	}

	@Test
	public void testLikeQueryOnMultiwordsNoMatch() throws Exception {
		//It is case-sensitive, as Analysis is disabled on wildcard queries:
		List<?> result = session.createQuery( "from Hypothesis h where h.description LIKE 'there are more than%'" ).list();
		assertThat( result ).isEmpty();
	}

	@Test
	public void testLikeQueryOnMultiwordsAsPrefix() throws Exception {
		//It is case-sensitive, as Analysis is disabled on wildcard queries:
		List<?> result = session.createQuery( "from Hypothesis h where h.description LIKE '%e'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "13", "14" );
	}

	@Test
	public void testLikeQueryOnMultiwordsPrefixed() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.description LIKE '%the cave'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "13" );
	}

	@Test
	public void testNegatedLikeQueryOnMultiwords() throws Exception {
		//Matching out:
		// "13" - "There are more than two dimensions over the shadows we see out of the cave"
		// "17" - "Is the truth out there?"
		// "18" - "The truth out there."
		// (first match gets excluded by multi-word negation)
		List<?> result = session.createQuery( "from Hypothesis h where h.description NOT LIKE '%out of%' and h.description LIKE '%out%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "17", "18" );
	}

	@Test
	public void testNotLikeQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.description NOT LIKE '%dimensions%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "14", "16", "17", "18", "19", "20" );
	}

	@Test
	public void testIsNullQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.description IS null" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "19" );
	}

	@Test
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Querying on embedded entities is not yet implemented.")
	public void testIsNullQueryOnPropertyEmbeddedEntity() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.author.name IS null" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "19" );
	}

	@Test
	public void testIsNotNullQuery() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.description IS NOT null" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "13", "14", "15", "16", "17", "18", "20" );
	}

	@Test
	@SkipByGridDialect(value = { MONGODB, NEO4J }, comment = "Querying on embedded entities is not yet implemented.")
	public void testIsNotNullQueryOnEmbeddedEntity() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.author IS NOT null" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( "14", "16", "19" );
	}

	@Test
	public void testGetNamedQuery() throws Exception {
		Helicopter result = (Helicopter) session.getNamedQuery( Helicopter.BY_NAME ).setParameter( "name", "Lama" ).uniqueResult();
		assertThat( result.getName() ).isEqualTo( "Lama" );
	}

	@Test
	@SkipByGridDialect(
			value = { EHCACHE, HASHMAP, INFINISPAN, COUCHDB, NEO4J },
			comment = "Should actually work with the Lucene backend; Presumably the tests fails though due to an "
					+ "unreliable order of returned elements. To be re-enabled once ORDER BY is implemented."
	)
	public void testFirstResultAndMaxRows() throws Exception {
		List<?> result = session.createQuery( "from Hypothesis h where h.description IS NOT null" )
				.setFirstResult( 2 )
				.setMaxResults( 3 )
				.list();
		assertThat( result ).onProperty( "id" ).containsOnly( "15", "16", "17" );
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

		Author alma = new Author();
		alma.setId( 2L );
		alma.setName( "alma" );
		alma.setAddress( mainStreet );
		session.persist( alma );

		Author withoutName = new Author();
		withoutName.setId( 3L );
		withoutName.setName( null );
		withoutName.setAddress( mainStreet );
		session.persist( withoutName );

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
		peano.setAuthor( alma );
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

		calendar.set( Calendar.YEAR, 2008 );
		Hypothesis truth = new Hypothesis();
		truth.setId( "17" );
		truth.setDescription( "Is the truth out there?" );
		truth.setPosition( 5 );
		truth.setDate( calendar.getTime() );
		session.persist( truth );

		calendar.set( Calendar.YEAR, 2007 );
		Hypothesis truthAnswer = new Hypothesis();
		truthAnswer.setId( "18" );
		truthAnswer.setDescription( "The truth out there." );
		truthAnswer.setPosition( 6 );
		truthAnswer.setDate( calendar.getTime() );
		session.persist( truthAnswer );

		calendar.set( Calendar.YEAR, 2006 );
		Hypothesis noDescription = new Hypothesis();
		noDescription.setId( "19" );
		noDescription.setDescription( null );
		noDescription.setAuthor( withoutName );
		noDescription.setPosition( 7 );
		noDescription.setDate( calendar.getTime() );
		session.persist( noDescription );

		Helicopter helicopter = new Helicopter();
		helicopter.setName( "No creative clue" );
		session.persist( helicopter );

		Helicopter anotherHelicopter = new Helicopter();
		anotherHelicopter.setName( "Lama" );
		session.persist( anotherHelicopter );

		Helicopter helicopterWithoutName = new Helicopter();
		session.persist( helicopterWithoutName );

		Hypothesis fool = new Hypothesis();
		fool.setId( "20" );
		fool.setDescription( "There are more than two fools in our team." );
		fool.setPosition( 4 );
		fool.setDate( calendar.getTime() );
		session.persist( fool );

		transaction.commit();
		session.close();
	}

	private void assertQuery(final Session session, final int expectedSize, final Query testedQuery) {
		List<?> list = testedQuery.list();
		assertThat( list ).as( "Query failed" ).hasSize( expectedSize );
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
