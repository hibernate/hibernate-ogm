/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.hibernate.ogm.utils.jpa.JpaTestCase;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class TableIdGeneratorTest extends JpaTestCase {

	@Test
	public void testTableIdGeneratorInJTA() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();
		Music music = new Music();
		music.setName( "Variations Sur Marilou" );
		music.setComposer( "Gainsbourg" );
		em.persist( music );
		Video video = new Video();
		video.setDirector( "Wes Craven" );
		video.setName( "Scream" );
		em.persist( video );
		getTransactionManager().commit();

		em.clear();

		getTransactionManager().begin();
		music = em.find( Music.class, music.getId() );
		assertThat( music ).isNotNull();
		assertThat( music.getName() ).isEqualTo( "Variations Sur Marilou" );
		em.remove( music );
		video = em.find( Video.class, video.getId() );
		assertThat( video ).isNotNull();
		assertThat( video.getName() ).isEqualTo( "Scream" );
		em.remove( video );
		getTransactionManager().commit();

		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] {
				Music.class,
				Video.class
		};
	}
}
