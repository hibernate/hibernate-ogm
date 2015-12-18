/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.hibernate.ogm.utils.OgmAssertions.assertThat;
import static org.hibernate.ogm.utils.SessionHelper.asProjectionResults;
import static org.hibernate.ogm.utils.SessionHelper.persist;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.SessionHelper.ProjectionResult;
import org.hibernate.ogm.utils.TestSessionFactory;
import org.hibernate.resource.transaction.spi.TransactionStatus;
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
		value = { GridDialectType.CASSANDRA, GridDialectType.INFINISPAN_REMOTE },
		comment = "Collection of embeddeds - bag semantics unsupported (no primary key)"
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
		if ( tx != null && tx.getStatus() == TransactionStatus.ACTIVE ) {
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
		List<?> result = session.createQuery( "from StoryGame e where e.goodBranch.storyText = 'you go to the [village]'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryWithInOperator() throws Exception {
		List<?> result = session.createQuery( "from StoryGame e where e.goodBranch.storyText IN ( 'you go to the [village]' )" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryWithBetstoryenOperator() throws Exception {
		List<?> result = session.createQuery( "from StoryGame e where e.goodBranch.ending.score BETWEEN 1 AND 6" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryWithLikeOperator() throws Exception {
		List<?> result = session.createQuery( "from StoryGame e where e.goodBranch.ending.text LIKE '[dungeon] end%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 300L );
	}

	@Test
	public void testQueryWithNestedEmbeddableInWhereClause() throws Exception {
		List<?> result = session.createQuery( "from StoryGame e where e.goodBranch.ending.text = '[village] ending - everybody is happy'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryWithComparisonOnMultipleProperties() throws Exception {
		List<?> result = session
				.createQuery( "from StoryGame e where e.evilBranch.storyText = 'evil branch - you kill everybody' AND e.goodBranch.ending.text = '[village] ending - everybody is happy'" )
				.list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryWithEmbeddablePropertyInSelectClauseWithOneResult() throws Exception {
		List<ProjectionResult> result = asProjectionResults( session, "select e.id, e.goodBranch.storyText from StoryGame e where e.id = 1" );
		assertThat( result ).containsOnly( new ProjectionResult( 1L, "you go to the [village]" ) );
	}

	@Test
	public void testQueryWithEmbeddablePropertyInSelectClause() throws Exception {
		List<ProjectionResult> result = asProjectionResults( session, "select e.id, e.evilBranch.storyText from StoryGame e" );
		assertThat( result ).containsOnly( new ProjectionResult( 1L, "evil branch - you kill everybody" ), new ProjectionResult( 20L, null ), new ProjectionResult( 300L, "evil branch - you become the [dungeon] keeper" ) );
	}

	@Test
	public void testQueryReturningEmbeddedObject() {
		List<?> list = session.createQuery( "FROM StoryGame story WHERE story.id = 1" ).list();

		assertThat( list )
			.onProperty( "goodBranch" )
			.onProperty( "storyText" )
			.containsExactly( "you go to the [village]" );

		assertThat( list )
			.onProperty( "goodBranch" )
			.onProperty( "ending" )
			.onProperty( "text" )
			.containsExactly( "[village] ending - everybody is happy" );

		assertThat( list )
			.onProperty( "evilBranch" )
			.onProperty( "storyText" )
			.containsExactly( "evil branch - you kill everybody" );
	}

	@BeforeClass
	public static void insertTestEntities() throws Exception {
		StoryGame story1 = new StoryGame( 1L, new StoryBranch( "you go to the [village]", new Ending( "[village] ending - everybody is happy", 1 ) ) );
		story1.setEvilBranch( new StoryBranch( "evil branch - you kill everybody", null ) );

		StoryGame story2 = new StoryGame( 20L, new StoryBranch( "you go the cave", new Ending( "cave ending - it's dark", 20 ) ) );
		story2.setEvilBranch( new StoryBranch( null, null ) );

		StoryGame story3 = new StoryGame( 300L, new StoryBranch( "you go to the [dungeon]", new Ending( "[dungeon] ending - you loot the treasures", 300 ) ) );
		story3.setEvilBranch( new StoryBranch( "evil branch - you become the [dungeon] keeper", null ) );

		persist( sessions, story1, story2, story3 );

	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StoryGame.class };
	}
}
