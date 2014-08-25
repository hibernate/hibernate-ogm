/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.associations.compositeid.Director;
import org.hibernate.ogm.backendtck.associations.compositeid.Tournament;
import org.hibernate.ogm.backendtck.associations.compositeid.TournamentId;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the mapping of associations with composite ids.
 *
 * @author Gunnar Morling
 */
public class ReferencedCompositeIdTest extends Neo4jJpaTestCase {

	@Before
	public void prepareDb() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();

		Tournament britishOpen = new Tournament( new TournamentId( "US", "123" ), "British Open" );
		Tournament playersChampionship = new Tournament( new TournamentId( "US", "456" ), "Player's Championship" );
		em.persist( britishOpen );
		em.persist( playersChampionship );

		Director bob = new Director( "bob", "Bob", playersChampionship );
		em.persist( bob );

		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 3 );
		assertRelationships( 1 );

		String director = "(d:Director:ENTITY { id: {d}.id, name: {d}.name })";
		String britishOpen = "(b:Tournament:ENTITY { `id.countryCode`: {b}.`id.countryCode`, `id.sequenceNo`: {b}.`id.sequenceNo`, name: {b}.name })";
		String players = "(p:Tournament:ENTITY { `id.countryCode`: {p}.`id.countryCode`, `id.sequenceNo`: {p}.`id.sequenceNo`, name: {p}.name })";

		Map<String, Object> directorProperties = new HashMap<String, Object>();
		directorProperties.put( "id", "bob" );
		directorProperties.put( "name", "Bob" );

		Map<String, Object> britishOpenProperties = new HashMap<String, Object>();
		britishOpenProperties.put( "id.countryCode", "US" );
		britishOpenProperties.put( "id.sequenceNo", "123" );
		britishOpenProperties.put( "name", "British Open" );

		Map<String, Object> playersProperties = new HashMap<String, Object>();
		playersProperties.put( "id.countryCode", "US" );
		playersProperties.put( "id.sequenceNo", "456" );
		playersProperties.put( "name", "Player's Championship" );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "d", directorProperties );
		params.put( "b", britishOpenProperties );
		params.put( "p", playersProperties );

		assertExpectedMapping( "d", director, params );
		assertExpectedMapping( "b", britishOpen, params );
		assertExpectedMapping( "p", players, params );
		assertExpectedMapping( "r", director + " - [r:directedTournament] - " + players, params );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { Director.class, Tournament.class, TournamentId.class };
	}
}
