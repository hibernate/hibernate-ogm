/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.hibernate.ogm.utils.OgmAssertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.GridDialectType;
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
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 * @author Gunnar Morling
 * @author Davide D'Alto
 */
@SkipByGridDialect(
		value = { GridDialectType.CASSANDRA },
		comment = "WithEmbedded list fields - bag semantics unsupported (no primary key)"
)
public class QueriesWithEmbeddedTest extends OgmTestCase {

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
	public void testQueryWithEmbeddableInWhereClause() throws Exception {
		List<?> result = session.createQuery( "from WithEmbedded e where e.anEmbeddable.embeddedString = 'string 1'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryWithInOperator() throws Exception {
		List<?> result = session.createQuery( "from WithEmbedded e where e.anEmbeddable.embeddedString IN ( 'string 1' )" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryWithBetweenOperator() throws Exception {
		List<?> result = session.createQuery( "from WithEmbedded e where e.anEmbeddable.anotherEmbeddable.embeddedInteger BETWEEN 1 AND 6" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryWithLikeOperator() throws Exception {
		List<?> result = session.createQuery( "from WithEmbedded e where e.anEmbeddable.anotherEmbeddable.embeddedString LIKE 'stri%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryWithNestedEmbeddableInWhereClause() throws Exception {
		List<?> result = session.createQuery( "from WithEmbedded e where e.anEmbeddable.anotherEmbeddable.embeddedString = 'string 2'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryWithComparisonOnMultipleProperties() throws Exception {
		List<?> result = session.createQuery( "from WithEmbedded e where e.yetAnotherEmbeddable.embeddedString = 'string 3' AND e.anEmbeddable.anotherEmbeddable.embeddedString = 'string 2'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryWithEmbeddablePropertyInSelectClause() throws Exception {
		List<ProjectionResult> result = asProjectionResults( "select e.id, e.anEmbeddable.embeddedString from WithEmbedded e" );
		assertThat( result ).containsOnly( new ProjectionResult( 1L, "string 1" ) );
	}

	@Test
	public void testQueryReturningEmbeddedObject() {
		List<?> list = session.createQuery( "from WithEmbedded we" ).list();

		assertThat( list )
			.onProperty( "anEmbeddable" )
			.onProperty( "embeddedString" )
			.containsExactly( "string 1" );

		assertThat( list )
			.onProperty( "anEmbeddable" )
			.onProperty( "anotherEmbeddable" )
			.onProperty( "embeddedString" )
			.containsExactly( "string 2" );

		assertThat( list )
			.onProperty( "yetAnotherEmbeddable" )
			.onProperty( "embeddedString" )
			.containsExactly( "string 3" );
	}

	@BeforeClass
	public static void insertTestEntities() throws Exception {
		WithEmbedded with = new WithEmbedded( 1L, new AnEmbeddable( "string 1", new AnotherEmbeddable( "string 2", 2 ) ) );
		with.setYetAnotherEmbeddable( new AnEmbeddable( "string 3", null ) );
		persist( with );
	}

	private static void persist(Object... entities) {
		final Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		for ( Object entity : entities ) {
			session.persist( entity );
		}

		transaction.commit();
		session.close();
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
		return new Class<?>[] { WithEmbedded.class };
	}
}
