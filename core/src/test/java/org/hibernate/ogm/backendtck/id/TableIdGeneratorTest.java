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

import org.hibernate.ogm.utils.jpa.JpaTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class TableIdGeneratorTest extends JpaTestCase {
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
	public void testTableIdGenerator() throws Exception {
		em.getTransaction().begin();
		Music music = new Music();
		music.setName( "Variations Sur Marilou" );
		music.setComposer( "Gainsbourg" );
		em.persist( music );
		Video video = new Video();
		video.setDirector( "Wes Craven" );
		video.setName( "Scream" );
		em.persist( video );
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		music = em.find( Music.class, music.getId() );
		assertThat( music ).isNotNull();
		assertThat( music.getName() ).isEqualTo( "Variations Sur Marilou" );
		em.remove( music );
		video = em.find( Video.class, video.getId() );
		assertThat( video ).isNotNull();
		assertThat( video.getName() ).isEqualTo( "Scream" );
		em.remove( video );
		em.getTransaction().commit();
	}

	@Test
	public void testTableIdGeneratorUsingLong() throws Exception {
		em.getTransaction().begin();
		Composer composer = new Composer();
		composer.setName( "Gainsbourg" );
		em.persist( composer );
		em.getTransaction().commit();

		em.clear();

		em.getTransaction().begin();
		composer = em.find( Composer.class, composer.getId() );
		assertThat( composer ).isNotNull();
		assertThat( composer.getName() ).isEqualTo( "Gainsbourg" );
		assertThat( composer.getId() ).isEqualTo( Integer.MAX_VALUE + 1 );
		em.remove( composer );
		em.getTransaction().commit();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Music.class,
				Video.class,
				Composer.class
		};
	}
}
