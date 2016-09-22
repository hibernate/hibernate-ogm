/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.EMBEDDED;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.queries.Ending;
import org.hibernate.ogm.backendtck.queries.OptionalStoryBranch;
import org.hibernate.ogm.backendtck.queries.StoryBranch;
import org.hibernate.ogm.backendtck.queries.StoryGame;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the mapping of embeddable collections in Neo4j
 *
 * @author Davide D'Alto
 */
public class ElementCollectionMappingTest extends Neo4jJpaTestCase {

	@Before
	public void prepareDB() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		List<Ending> goodBranchAdditionalEndings = new ArrayList<Ending>();
		goodBranchAdditionalEndings.add( new Ending( "Bonus ending - you save the world", 100 ) );
		goodBranchAdditionalEndings.add( new Ending( "Bonus ending - you kill the demon", 80 ) );

		StoryBranch goodBranch = new StoryBranch( "you go to the village", new Ending( "village ending - everybody is happy", 1 ) );
		goodBranch.setAdditionalEndings( goodBranchAdditionalEndings );

		StoryBranch evilBranch = new StoryBranch( "you kill the villagers" );

		StoryGame story = new StoryGame( 1L, null );
		story.setGoodBranch( goodBranch );
		story.setEvilBranch( evilBranch );

		story.setChaoticBranches( Arrays.asList(
				new OptionalStoryBranch( "search the evil [artifact]", "you punish the bandits", null ),
				new OptionalStoryBranch( "assassinate the leader of the party", null, new Ending( "you become a demon", 10 ) ) ) );

		story.setNeutralBranches( Arrays.asList(
				new OptionalStoryBranch( "steal the [artifact]", null, null ),
				new OptionalStoryBranch( "kill the king", null, null ) ) );

		em.persist( story );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testEmbeddedCollectionNodesMapping() throws Exception {

		NodeForGraphAssertions storyGameNode = node( "story", StoryGame.class.getSimpleName(), ENTITY.name() )
				.property( "id", 1L );

		NodeForGraphAssertions goodBranchNode = node( "good", EMBEDDED.name() )
				.property( "storyText", "you go to the village" );

		NodeForGraphAssertions goodBranchEndingNode = node( "goodEnd", EMBEDDED.name() )
				.property( "text", "village ending - everybody is happy" )
				.property( "score", 1 );

		NodeForGraphAssertions goodBranchAdditionalEndingNode1 = node( "goodEndAdd1", EMBEDDED.name() )
				.property( "text", "Bonus ending - you save the world" )
				.property( "score", 100 );

		NodeForGraphAssertions goodBranchAdditionalEndingNode2 = node( "goodEndAdd2", EMBEDDED.name() )
				.property( "text", "Bonus ending - you kill the demon" )
				.property( "score", 80 );

		NodeForGraphAssertions evilBranchNode = node( "evil", EMBEDDED.name() )
				.property( "storyText", "you kill the villagers" );

		NodeForGraphAssertions chaoticBranchNode1 = node( "chaos1", "StoryGame_chaoticBranches", EMBEDDED.name() )
				.property( "evilText", "assassinate the leader of the party" );

		NodeForGraphAssertions chaoticBranchNode1EvilEnding = node( "chaos1End", EMBEDDED.name() )
				.property( "text", "you become a demon" )
				.property( "score", 10 );

		NodeForGraphAssertions chaoticBranchNode2 = node( "chaos2", "StoryGame_chaoticBranches", EMBEDDED.name() )
				.property( "evilText", "search the evil [artifact]" )
				.property( "goodText", "you punish the bandits" );

		NodeForGraphAssertions neutralBranchNode1 = node( "neutral1", "StoryGame_neutralBranches", EMBEDDED.name() )
				.property( "evilText", "steal the [artifact]" );

		NodeForGraphAssertions neutralBranchNode2 = node( "neutral1", "StoryGame_neutralBranches", EMBEDDED.name() )
				.property( "evilText", "kill the king" );

		RelationshipsChainForGraphAssertions relationship1 =
				storyGameNode
					.relationshipTo( goodBranchNode, "goodBranch" )
					.relationshipTo( goodBranchEndingNode, "ending" );

		RelationshipsChainForGraphAssertions relationship2 =
				goodBranchNode
					.relationshipTo( goodBranchAdditionalEndingNode1, "additionalEndings" );

		RelationshipsChainForGraphAssertions relationship3 =
				goodBranchNode
					.relationshipTo( goodBranchAdditionalEndingNode2, "additionalEndings" );

		RelationshipsChainForGraphAssertions relationship4 =
				storyGameNode
					.relationshipTo( evilBranchNode, "evilBranch" );

		RelationshipsChainForGraphAssertions relationship5 =
				storyGameNode
					.relationshipTo( chaoticBranchNode1, "chaoticBranches" )
					.relationshipTo( chaoticBranchNode1EvilEnding, "evilEnding" );

		RelationshipsChainForGraphAssertions relationship6 =
				storyGameNode
					.relationshipTo( chaoticBranchNode2, "chaoticBranches" );

		RelationshipsChainForGraphAssertions relationship7 =
				storyGameNode
					.relationshipTo( neutralBranchNode1, "neutralBranches" );

		RelationshipsChainForGraphAssertions relationship8 =
				storyGameNode
					.relationshipTo( neutralBranchNode2, "neutralBranches" );

		assertThatOnlyTheseNodesExist(
				storyGameNode
				, goodBranchNode
				, goodBranchEndingNode
				, goodBranchAdditionalEndingNode1
				, goodBranchAdditionalEndingNode2
				, evilBranchNode
				, chaoticBranchNode1
				, chaoticBranchNode1EvilEnding
				, chaoticBranchNode2
				, neutralBranchNode1
				, neutralBranchNode2
		);

		assertThatOnlyTheseRelationshipsExist(
				relationship1
				, relationship2
				, relationship3
				, relationship4
				, relationship5
				, relationship6
				, relationship7
				, relationship8
		);
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { StoryGame.class };
	}
}
