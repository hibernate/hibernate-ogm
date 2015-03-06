/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.ogm.utils.jpa.JpaTestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests that the insertion of a record with an already existing primary key is prevented.
 *
 * @author Gunnar Morling
 */
public class DuplicateIdDetectionTest extends JpaTestCase {
	EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		em.close();
	}

	@Test
	public void cannotInsertSameEntityTwice() throws Exception {
		em.getTransaction().begin();

		// given
		MakeupArtist wibke = new MakeupArtist( "wibke", "halloween" );
		em.persist( wibke );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		// when
		MakeupArtist notWibke = new MakeupArtist( "wibke", "glamorous" );
		em.persist( notWibke );

		try {
			em.getTransaction().commit();
			fail( "Expected exception wasn't raised" );
		}
		catch ( Exception e ) {
			// then
			assertThat( e.getCause() ).isExactlyInstanceOf( PersistenceException.class );
			assertThat( e.getCause().getMessage() ).matches( ".*OGM000067.*" );
		}

		em.clear();

		em.getTransaction().begin();
		MakeupArtist loadedMakeupArtist = em.find( MakeupArtist.class, "wibke" );
		assertThat( loadedMakeupArtist ).isNotNull();
		assertThat( loadedMakeupArtist.getFavoriteStyle() ).describedAs( "Second insert should not be applied" )
				.isEqualTo( "halloween" );

		em.remove( loadedMakeupArtist );
		em.getTransaction().commit();
	}

	@Test
	public void cannotInsertSameEntityUsingCompositeKeyTwice() throws Exception {
		em.getTransaction().begin();

		// given
		MakeupArtistWithCompositeKey wibke = new MakeupArtistWithCompositeKey(
				new MakeUpArtistId( "fancy-film", "wibke" ), "halloween"
		);
		em.persist( wibke );

		em.getTransaction().commit();
		em.clear();
		em.getTransaction().begin();

		// when
		MakeupArtistWithCompositeKey notWibke = new MakeupArtistWithCompositeKey(
				new MakeUpArtistId( "fancy-film", "wibke" ), "glamorous"
		);
		em.persist( notWibke );

		try {
			em.getTransaction().commit();
			fail( "Expected exception wasn't raised" );
		}
		catch ( Exception e ) {
			// then
			assertThat( e.getCause().getMessage() ).matches( ".*OGM000067.*" );
		}

		em.clear();
		em.getTransaction().begin();

		MakeupArtistWithCompositeKey loadedMakeupArtist = em.find(
				MakeupArtistWithCompositeKey.class, new MakeUpArtistId( "fancy-film", "wibke" )
		);
		assertThat( loadedMakeupArtist ).isNotNull();
		assertThat( loadedMakeupArtist.getFavoriteStyle() ).describedAs( "Second insert should not be applied" )
				.isEqualTo( "halloween" );

		em.remove( loadedMakeupArtist );
		em.getTransaction().commit();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { MakeupArtist.class, MakeupArtistWithCompositeKey.class };
	}
}
