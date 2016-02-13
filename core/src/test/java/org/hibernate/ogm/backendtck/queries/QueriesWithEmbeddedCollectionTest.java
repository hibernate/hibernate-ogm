/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.hibernate.ogm.utils.OgmAssertions.assertThat;
import static org.hibernate.ogm.utils.SessionHelper.asProjectionResults;
import static org.hibernate.ogm.utils.SessionHelper.delete;
import static org.hibernate.ogm.utils.SessionHelper.persist;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SessionHelper.ProjectionResult;
import org.hibernate.ogm.utils.SkipByDatastoreProvider;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestSessionFactory;
import org.hibernate.resource.transaction.spi.TransactionStatus;
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
@SkipByGridDialect(value = GridDialectType.CASSANDRA, comment = "Bag semantics not supported by Cassandra backend")
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
	public void testEqualOpeartorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText = '[ARTIFACT] Search for the evil artifact'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testInOperatorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "from StoryGame story JOIN story.chaoticBranches c WHERE c.evilText IN ( '[ARTIFACT] Search for the evil artifact' )" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}


	@Test
	public void testBetweenOpeartorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText BETWEEN '[' AND '[B'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testLikeOpeartorWithEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText LIKE '[ART%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testEqualEmbeddedCollectionWithEmbeddableInCollectionWhereClause() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText = '[VENDETTA] assassinate the leader of the party'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testConjunctionOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText = '[ARTIFACT] Search for the evil artifact' AND c.goodText IN ('[BANDITS] you punish the bandits')" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testBetweenOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilEnding.score BETWEEN -100 AND 100" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testLikeOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilEnding.text LIKE '[VENDETT%'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testEqualOperatorWithEmbeddedInEmbeddedCollectionForString() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilEnding.text = '[VENDETTA] you become a demon'" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testEqualOperatorWithEmbeddedInEmbeddedCollectionForInteger() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilEnding.score = 10" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	public void testConjunctionOperatorEqualOperatorWithEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "FROM StoryGame story JOIN story.chaoticBranches c WHERE c.evilText = '[VENDETTA] assassinate the leader of the party' AND c.evilEnding.text IN ('[VENDETTA] you become a demon')" ).list();
		assertThat( result ).onProperty( "id" ).containsOnly( 1L );
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.CASSANDRA, GridDialectType.COUCHDB, GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.REDIS_JSON, GridDialectType.REDIS_HASH },
			comment = "Hibernate Search cannot project multiple values from the same field at the moment" )
	@SkipByDatastoreProvider(value = AvailableDatastoreProvider.FONGO, comment = "OGM-835 - needs a Fongo upgrade (once avialable)")
	public void testProjectionsOfPropertyInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "SELECT c.evilText FROM StoryGame story JOIN story.chaoticBranches c WHERE story.id = 1" ).list();
		assertThat( result ).containsOnly( "[ARTIFACT] Search for the evil artifact", "[VENDETTA] assassinate the leader of the party" );
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.CASSANDRA, GridDialectType.COUCHDB, GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.REDIS_JSON, GridDialectType.REDIS_HASH },
			comment = "Hibernate Search cannot project multiple values from the same field at the moment" )
	@SkipByDatastoreProvider(value = AvailableDatastoreProvider.FONGO, comment = "OGM-835 - needs a Fongo upgrade (once avialable)")
	public void testProjectionsOfEmbeddedInEmbeddedCollection() throws Exception {
		List<?> result = session.createQuery( "SELECT c.evilEnding.score FROM StoryGame story JOIN story.chaoticBranches c WHERE story.id = 1" ).list();
		assertThat( result ).containsOnly( 5, 10 );
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.CASSANDRA, GridDialectType.COUCHDB, GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.REDIS_JSON, GridDialectType.REDIS_HASH },
			comment = "Hibernate Search cannot project multiple values from the same field at the moment" )
	@SkipByDatastoreProvider(value = AvailableDatastoreProvider.FONGO, comment = "OGM-835 - needs a Fongo upgrade (once avialable)")
	public void testProjectionsOfEmbeddedInEmbeddedCollectionWithNull() throws Exception {
		List<?> result = session.createQuery( "SELECT c.evilEnding.score FROM StoryGame story JOIN story.chaoticBranches c WHERE story.id = 20" ).list();
		assertThat( result ).containsOnly( null, 333 );
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.CASSANDRA, GridDialectType.COUCHDB, GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.REDIS_JSON, GridDialectType.REDIS_HASH },
			comment = "Hibernate Search cannot project multiple values from the same field at the moment" )
	@SkipByDatastoreProvider(value = AvailableDatastoreProvider.FONGO, comment = "OGM-835 - needs a Fongo upgrade (once avialable)")
	public void testProjectionsOfPropertiesInEmbeddedCollection() throws Exception {
		List<ProjectionResult> result = asProjectionResults( session, "SELECT story.id, story.goodBranch.storyText, c.evilEnding.text, c.evilText FROM StoryGame story JOIN story.chaoticBranches c WHERE story.id = 1" );
		assertThat( result ).containsOnly(
				new ProjectionResult( 1L, "[VILLAGE] You go to the village", "[ARTIFACT] You succumb to the dark side", "[ARTIFACT] Search for the evil artifact" ),
				new ProjectionResult( 1L, "[VILLAGE] You go to the village", "[VENDETTA] you become a demon", "[VENDETTA] assassinate the leader of the party" ) );
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.CASSANDRA, GridDialectType.COUCHDB, GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.REDIS_JSON, GridDialectType.REDIS_HASH },
			comment = "Hibernate Search cannot project multiple values from the same field at the moment" )
	@SkipByDatastoreProvider(value = AvailableDatastoreProvider.FONGO, comment = "OGM-835 - needs a Fongo upgrade (once avialable)")
	public void testProjectionsOfPropertiesInEmbeddedCollectionWithInnerJoin() throws Exception {
		List<ProjectionResult> result = asProjectionResults( session, "SELECT story.id, story.goodBranch.storyText, c.evilEnding.text, c.evilText FROM StoryGame story JOIN story.chaoticBranches c " );
		assertThat( result ).containsOnly(
				new ProjectionResult( 1L, "[VILLAGE] You go to the village", "[ARTIFACT] You succumb to the dark side", "[ARTIFACT] Search for the evil artifact" ),
				new ProjectionResult( 1L, "[VILLAGE] You go to the village", "[VENDETTA] you become a demon", "[VENDETTA] assassinate the leader of the party" ),
				new ProjectionResult( 20L, "[CAVE] You go the cave", null, "[KING] Ask for your help" ),
				new ProjectionResult( 20L, "[CAVE] You go the cave", "[WEREWOLF] Sometimes people hear you howl at the moon", "[WEREWOLF] You become a werewolf" ) );
	}

	@Test
	@SkipByGridDialect(
			value = { GridDialectType.CASSANDRA, GridDialectType.COUCHDB, GridDialectType.EHCACHE, GridDialectType.HASHMAP, GridDialectType.INFINISPAN, GridDialectType.REDIS_JSON, GridDialectType.REDIS_HASH },
			comment = "Hibernate Search cannot project multiple values from the same field at the moment" )
	@SkipByDatastoreProvider(value = AvailableDatastoreProvider.FONGO, comment = "OGM-835 - needs a Fongo upgrade (once avialable)")
	public void testProjectionWithMultipleAssociations() throws Exception {
		List<ProjectionResult> result = asProjectionResults( session, "SELECT story.id, c.evilEnding.text, n.evilText "
				+ "FROM StoryGame story JOIN story.chaoticBranches c JOIN story.neutralBranches n WHERE story.id = 1" );
		assertThat( result ).containsOnly(
				new ProjectionResult( 1L, "[ARTIFACT] You succumb to the dark side", "[VENDETTA] Kill the king" ),
				new ProjectionResult( 1L, "[ARTIFACT] You succumb to the dark side", "[THIEF] steal the artifact" ),
				new ProjectionResult( 1L, "[VENDETTA] you become a demon", "[VENDETTA] Kill the king" ),
				new ProjectionResult( 1L, "[VENDETTA] you become a demon", "[THIEF] steal the artifact" ) );
	}

	@Test
	public void testQueryReturningEmbeddedObject() {
		@SuppressWarnings("unchecked")
		List<StoryGame> list = session.createQuery( "FROM StoryGame story WHERE story.id = 1" ).list();

		assertThat( list )
			.onProperty( "goodBranch" )
			.onProperty( "storyText" )
			.containsExactly( "[VILLAGE] You go to the village" );

		assertThat( list )
			.onProperty( "goodBranch" )
			.onProperty( "ending" )
			.onProperty( "text" )
			.containsExactly( "[VILLAGE] Everybody is happy" );

		List<Ending> additionalEndings = list.get( 0 ).getGoodBranch().getAdditionalEndings();
		assertThat( additionalEndings )
			.onProperty( "text" )
			.containsOnly( "[DRAGON] Slay the dragon" );

		assertThat( additionalEndings )
			.onProperty( "score" )
		.	containsOnly( 5 );

		assertThat( list )
			.onProperty( "evilBranch" )
			.onProperty( "storyText" )
			.containsExactly( "[EVIL] you kill the villagers" );

		List<OptionalStoryBranch> chaoticBranches = list.get( 0 ).getChaoticBranches();
		assertThat( chaoticBranches )
			.onProperty( "evilText" )
			.containsOnly( "[ARTIFACT] Search for the evil artifact", "[VENDETTA] assassinate the leader of the party" );

		assertThat( chaoticBranches )
			.onProperty( "evilEnding" )
			.containsOnly( new Ending( "[ARTIFACT] You succumb to the dark side", 5 ), new Ending( "[VENDETTA] you become a demon", 10 ) );

		assertThat( chaoticBranches )
			.onProperty( "goodText" )
			.containsOnly( "[BANDITS] you punish the bandits", null );

		List<OptionalStoryBranch> neutralBranches = list.get( 0 ).getNeutralBranches();
		assertThat( neutralBranches )
			.onProperty( "evilText" )
			.containsOnly( "[THIEF] steal the artifact", "[VENDETTA] Kill the king" );

		assertThat( neutralBranches )
			.onProperty( "goodText" )
			.containsOnly( null, null );
	}

	@BeforeClass
	public static void insertTestEntities() throws Exception {
		StoryGame story1 = new StoryGame( 1L, null );

		StoryBranch story1GoodBranch = new StoryBranch( "[VILLAGE] You go to the village", new Ending( "[VILLAGE] Everybody is happy", 1 ) );
		story1GoodBranch.setAdditionalEndings( Arrays.asList( new Ending( "[DRAGON] Slay the dragon", 5 ) ) );

		story1.setGoodBranch( story1GoodBranch );
		story1.setEvilBranch( new StoryBranch( "[EVIL] you kill the villagers" ) );
		story1.setChaoticBranches( Arrays.asList(
				new OptionalStoryBranch( "[ARTIFACT] Search for the evil artifact", "[BANDITS] you punish the bandits", new Ending( "[ARTIFACT] You succumb to the dark side", 5 ) ),
				new OptionalStoryBranch( "[VENDETTA] assassinate the leader of the party", null, new Ending( "[VENDETTA] you become a demon", 10 ) ) ) );

		story1.setNeutralBranches( Arrays.asList(
				new OptionalStoryBranch( "[THIEF] steal the artifact", null, null ),
				new OptionalStoryBranch( "[VENDETTA] Kill the king", null, null ) ) );

		StoryGame story2 = new StoryGame( 20L, new StoryBranch( "[CAVE] You go the cave", new Ending( "[CAVE] It's dark", 20 ) ) );
		story2.setEvilBranch( new StoryBranch( "[VAMPIRE] You are now a vampire", null ) );
		story2.setChaoticBranches( Arrays.asList(
				new OptionalStoryBranch( "[KING] Ask for your help", "[BLACKSMITH] The blacksmith send you for a quest", null ),
				new OptionalStoryBranch( "[WEREWOLF] You become a werewolf", null, new Ending( "[WEREWOLF] Sometimes people hear you howl at the moon", 333 ) ) ) );

		StoryGame story3 = new StoryGame( 300L, new StoryBranch( "[DUNGEON] You go to the dungeon", null ) );
		story3.setEvilBranch( new StoryBranch( "[DUNGEON] You become the dungeon keeper", null ) );

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
