/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.compositeid.Director;
import org.hibernate.ogm.backendtck.associations.compositeid.Tournament;
import org.hibernate.ogm.backendtck.associations.compositeid.TournamentId;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.dsl.RelationshipsChainForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the mapping of associations with composite ids.
 *
 * @author Gunnar Morling
 */
public class ReferencedCompositeIdTest extends Neo4jJpaTestCase {

	private Director director;
	private Tournament britishOpen;
	private Tournament playersChampionship;

	@Before
	public void prepareDb() throws Exception {
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		britishOpen = new Tournament( new TournamentId( "US", "123" ), "British Open" );
		playersChampionship = new Tournament( new TournamentId( "US", "456" ), "Player's Championship" );
		em.persist( britishOpen );
		em.persist( playersChampionship );

		director = new Director( "bob", "Bob", playersChampionship );
		em.persist( director );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions directorNode = node( "director", Director.class.getSimpleName(), ENTITY.name() )
				.property( "id", director.getId() )
				.property( "name", director.getName() );

		NodeForGraphAssertions britishOpenNode = node( "british", Tournament.class.getSimpleName(), ENTITY.name() )
				.property( "id.countryCode", britishOpen.getId().getCountryCode() )
				.property( "id.sequenceNo", britishOpen.getId().getSequenceNo() )
				.property( "name", britishOpen.getName() );

		NodeForGraphAssertions playersChampionshipNode = node( "playerChamp", Tournament.class.getSimpleName(), ENTITY.name() )
				.property( "id.countryCode", playersChampionship.getId().getCountryCode() )
				.property( "id.sequenceNo", playersChampionship.getId().getSequenceNo() )
				.property( "name", playersChampionship.getName() );

		RelationshipsChainForGraphAssertions relationship1 = directorNode.relationshipTo( playersChampionshipNode, "directedTournament" );

		assertThatOnlyTheseNodesExist( directorNode, britishOpenNode, playersChampionshipNode );
		assertThatOnlyTheseRelationshipsExist( relationship1 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Director.class, Tournament.class, TournamentId.class };
	}
}
