/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.SEQUENCE;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;

import javax.persistence.EntityManager;

import org.hibernate.ogm.backendtck.id.Song;
import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
public class SingleEntityWithSequenceTest extends Neo4jJpaTestCase {

	private Song song;

	@Before
	public void prepareDb() throws Exception {
		final EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		song = new Song();
		song.setSinger( "Jon Bovi" );
		song.setTitle( "Keep the pace" );
		em.persist( song );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMapping() throws Exception {
		NodeForGraphAssertions jugNode = node( "song", Song.class.getSimpleName(), ENTITY.name() )
				.property( "id", song.getId() )
				.property( "singer", song.getSinger() )
				.property( "title", song.getTitle() );

		NodeForGraphAssertions sequenceNode = node( "sequence", SEQUENCE.name() )
				.property( "sequence_name", "song_sequence_name" )
				.property( "next_val", 22 );

		assertThatOnlyTheseNodesExist( jugNode, sequenceNode );
		assertNumberOfRelationships( 0 );

		assertNumberOfNodes( 2 );
		assertNumberOfRelationships( 0 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Song.class };
	}

}
