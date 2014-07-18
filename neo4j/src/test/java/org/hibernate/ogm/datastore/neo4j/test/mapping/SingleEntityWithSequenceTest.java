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

		Map<String, Object> songProperties = new HashMap<String, Object>();
		songProperties.put( "id", song.getId() );
		songProperties.put( "singer", song.getSinger() );
		songProperties.put( "title", song.getTitle() );

		Map<String, Object> sequenceProperties = new HashMap<String, Object>();
		sequenceProperties.put( "sequence_name", "song_sequence_name" );
		sequenceProperties.put( "next_val", 22 );

		Map<String, Object> params = new HashMap<String, Object>();
		params.put( "song", songProperties );
		params.put( "sq", sequenceProperties );

		assertExpectedMapping( "song", "(song:Song:ENTITY {id: {song}.id, singer: {song}.singer, title: {song}.title })", params );
		assertExpectedMapping( "sq", "(sq:SEQUENCE{sequence_name: {sq}.sequence_name, next_val: {sq}.next_val})", params );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class[] { Song.class };
	}

}
