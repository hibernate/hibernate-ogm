/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.id;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.Test;

/**
 * Tests that the insertion of a record with an already existing primary key is prevented.
 *
 * @author Gunnar Morling
 */
public class DuplicateIdDetectionTest extends JpaTestCase {

	@Test
	public void cannotInsertSameEntityTwice() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();

		// given
		MakeupArtist wibke = new MakeupArtist( "wibke", "halloween" );
		em.persist( wibke );

		getTransactionManager().commit();
		em.clear();
		getTransactionManager().begin();
		em.joinTransaction();

		// when
		MakeupArtist notWibke = new MakeupArtist( "wibke", "glamorous" );
		em.persist( notWibke );

		try {
			getTransactionManager().commit();
			fail( "Expected exception wasn't raised" );
		}
		catch (Exception e) {
			// then
			assertThat( e.getCause() ).isExactlyInstanceOf( EntityExistsException.class );
			assertThat( e.getCause().getMessage() ).matches( ".*OGM000067.*" );
		}

		em.clear();
		getTransactionManager().begin();
		em.joinTransaction();

		MakeupArtist loadedMakeupArtist = em.find( MakeupArtist.class, "wibke" );
		assertThat( loadedMakeupArtist ).isNotNull();
		assertThat( loadedMakeupArtist.getFavoriteStyle() ).describedAs( "Second insert should not be applied" ).isEqualTo( "halloween" );

		em.remove( loadedMakeupArtist );
		getTransactionManager().commit();
		em.close();
	}

	@Test
	public void cannotInsertSameEntityUsingCompositeKeyTwice() throws Exception {
		getTransactionManager().begin();
		EntityManager em = getFactory().createEntityManager();

		// given
		MakeupArtistWithCompositeKey wibke = new MakeupArtistWithCompositeKey( new MakeUpArtistId( "fancy-film", "wibke" ), "halloween" );
		em.persist( wibke );

		getTransactionManager().commit();
		em.clear();
		getTransactionManager().begin();
		em.joinTransaction();

		// when
		MakeupArtistWithCompositeKey notWibke = new MakeupArtistWithCompositeKey( new MakeUpArtistId( "fancy-film", "wibke" ), "glamorous" );
		em.persist( notWibke );

		try {
			getTransactionManager().commit();
			fail( "Expected exception wasn't raised" );
		}
		catch (Exception e) {
			// then
			assertThat( e.getCause().getMessage() ).matches( ".*OGM000067.*" );
		}

		em.clear();
		getTransactionManager().begin();
		em.joinTransaction();

		MakeupArtistWithCompositeKey loadedMakeupArtist = em.find( MakeupArtistWithCompositeKey.class, new MakeUpArtistId( "fancy-film", "wibke" ) );
		assertThat( loadedMakeupArtist ).isNotNull();
		assertThat( loadedMakeupArtist.getFavoriteStyle() ).describedAs( "Second insert should not be applied" ).isEqualTo( "halloween" );

		em.remove( loadedMakeupArtist );
		getTransactionManager().commit();
		em.close();
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { MakeupArtist.class, MakeupArtistWithCompositeKey.class };
	}
}
