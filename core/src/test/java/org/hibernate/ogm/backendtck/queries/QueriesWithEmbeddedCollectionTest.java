/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.hibernate.ogm.utils.OgmAssertions.assertThat;
import static org.hibernate.ogm.utils.SessionHelper.delete;
import static org.hibernate.ogm.utils.SessionHelper.persist;

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
import org.junit.AfterClass;
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
		comment = "WithEmbedded has lists - bag semantics unsupported (no primary key)"
)
public class QueriesWithEmbeddedCollectionTest extends OgmTestCase {

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
	public void testEqualOpeartorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText = 'search the evil [artifact]'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testInOperatorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "from StoryGame story JOIN story.chaoticBranches c WHERE c.evilText IN ( 'search the evil [artifact]' )" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}


	@Test
	public void testBetweenOpeartorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText BETWEEN 'aaaaaa' AND 'zzzzzzzzz'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testLikeOpeartorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText LIKE 'search the%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testEqualEmbeddedCollectionWithEmbeddableInCollectionWhereClause() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText = 'assassinate the leader of the party'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testConjunctionOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText = 'search the evil [artifact]' AND c.goodText IN ('you punish the bandits')" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testBetweenOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilEnding.score BETWEEN -100 AND 100" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testLikeOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilEnding.text LIKE 'you bec%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testEqualOperatorWithEmbeddedInEmbeddedCollectionForString() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilEnding.text = 'you become a demon'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testEqualOperatorWithEmbeddedInEmbeddedCollectionForInteger() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilEnding.score = 10" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testConjunctionOperatorEqualOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText = 'assassinate the leader of the party' AND c.evilEnding.text IN ('you become a demon')" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testQueryReturningEmbeddedObject() {
		@SuppressWarnings("unchecked")
		List<StoryGame> list = session.createQuery( "FROM StoryGame story WHERE story.id = 1" ).list();

		assertThat( list )
			.onProperty( "goodBranch" )
			.onProperty( "storyText" )
			.containsExactly( "you go to the village" );

		assertThat( list )
			.onProperty( "goodBranch" )
			.onProperty( "ending" )
			.onProperty( "text" )
			.containsExactly( "village ending - everybody is happy" );

		assertThat( list )
			.onProperty( "evilBranch" )
			.onProperty( "storyText" )
			.containsExactly( "you kill the villagers" );

		assertThat( list.get( 0 ).getChaoticBranches() )
			.onProperty( "evilText" )
			.containsOnly( "search the evil [artifact]", "assassinate the leader of the party" );

		assertThat( list.get( 0 ).getChaoticBranches() )
			.onProperty( "goodText" )
			.containsOnly( "you punish the bandits", null );

		assertThat( list.get( 0 ).getNeutralBranches() )
			.onProperty( "evilText" )
			.containsOnly( "steal the [artifact]", "kill the king" );

		assertThat( list.get( 0 ).getNeutralBranches() )
			.onProperty( "goodText" )
			.containsOnly( null, null );
	}

	@BeforeClass
	public static void insertTestEntities() throws Exception {
		StoryGame story1 = new StoryGame( 1L, null );
		story1.setGoodBranch( new StoryBranch( "you go to the village", new Ending( "village ending - everybody is happy", 1 ) ) );
		story1.setEvilBranch( new StoryBranch( "you kill the villagers" ) );
		story1.setChaoticBranches( Arrays.asList(
				new OptionalStoryBranch( "search the evil [artifact]", "you punish the bandits", null ),
				new OptionalStoryBranch( "assassinate the leader of the party", null, new Ending( "you become a demon", 10 ) ) ) );

		story1.setNeutralBranches( Arrays.asList(
				new OptionalStoryBranch( "steal the [artifact]", null, null ),
				new OptionalStoryBranch( "kill the king", null, null ) ) );

		StoryGame story2 = new StoryGame( 20L, new StoryBranch( "you go the [cave]", new Ending( "[cave] ending - it's dark", 20 ) ) );
		story2.setEvilBranch( new StoryBranch( "you are now a vampire", null ) );

		StoryGame story3 = new StoryGame( 300L, new StoryBranch( "you go to the [dungeon]", new Ending( "[dungeon] ending - you loot the treasures", 300 ) ) );
		story3.setEvilBranch( new StoryBranch( "evil branch - you become the [dungeon] keeper", null ) );

		persist( sessions, story1, story2, story3);
	}


	@AfterClass
	public static void removeTestEntities() throws Exception {
		delete( sessions, StoryGame.class, 1L, 20L, 300L );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StoryGame.class };
	}
}
