/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Nabeel Ali Memon &lt;nabeel@nabeelalimemon.com&gt;
 */
public class SequenceIdGeneratorTest extends OgmJpaTestCase {
	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		em.close();
	}

	@Test
	public void testSequenceIdGenerationInJTA() throws Exception {
		Song firstSong = new Song();
		Song secondSong = new Song();
		Actor firstActor = new Actor();
		Actor secondActor = new Actor();

		em.getTransaction().begin();
		firstSong.setSinger( "Charlotte Church" );
		firstSong.setTitle( "Ave Maria" );
		em.persist( firstSong );

		secondSong.setSinger( "Charlotte Church" );
		secondSong.setTitle( "Flower Duet" );
		em.persist( secondSong );

		firstActor.setName( "Russell Crowe" );
		firstActor.setBestMovieTitle( "Gladiator" );
		em.persist( firstActor );

		secondActor.setName( "Johnny Depp" );
		secondActor.setBestMovieTitle( "Pirates of the Caribbean" );
		em.persist( secondActor );
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		firstSong = em.find( Song.class, firstSong.getId() );
		assertThat( firstSong ).isNotNull();
		assertThat( firstSong.getId() ).isEqualTo( Song.INITIAL_VALUE );
		assertThat( firstSong.getTitle() ).isEqualTo( "Ave Maria" );
		em.remove( firstSong );

		secondSong = em.find( Song.class, secondSong.getId() );
		assertThat( secondSong ).isNotNull();
		assertThat( secondSong.getId() ).isEqualTo( Song.INITIAL_VALUE + 1 );
		assertThat( secondSong.getTitle() ).isEqualTo( "Flower Duet" );
		em.remove( secondSong );

		firstActor = em.find( Actor.class, firstActor.getId() );
		assertThat( firstActor ).isNotNull();
		assertThat( firstActor.getId() ).isEqualTo( Actor.INITIAL_VALUE );
		assertThat( firstActor.getName() ).isEqualTo( "Russell Crowe" );
		em.remove( firstActor );

		secondActor = em.find( Actor.class, secondActor.getId() );
		assertThat( secondActor ).isNotNull();
		assertThat( secondActor.getId() ).isEqualTo( Actor.INITIAL_VALUE + 1 );
		assertThat( secondActor.getName() ).isEqualTo( "Johnny Depp" );
		em.remove( secondActor );
		em.getTransaction().commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Song.class,
				Actor.class
		};
	}
}
