/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.id.Song;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class SingleEntityWithSequenceTest extends Neo4jJpaTestCase {

	private Song song;

	@Before
	public void prepareDb() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		song = new Song();
		song.setSinger( "Jon Bovi" );
		song.setTitle( "Keep the pace" );
		em.persist( song );
		commitOrRollback( true );
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		assertNumberOfNodes( 2 );
		assertRelationships( 0 );
		assertExpectedMapping( "(:Song:ENTITY {id: " + song.getId() + ", singer: '" + song.getSinger() + "', title: '" + song.getTitle() + "' })" );
		assertExpectedMapping( "(:SEQUENCE { sequence_name: 'song_sequence_name' })" );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { Song.class };
	}

}
