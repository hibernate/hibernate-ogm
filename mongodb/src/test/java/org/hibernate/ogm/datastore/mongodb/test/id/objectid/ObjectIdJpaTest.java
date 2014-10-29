/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.objectid;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.EntityManager;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.ogm.utils.jpa.JpaTestCase;
import org.junit.Test;

/**
 * Tests for using object ids with MongoDB.
 *
 * @author Gunnar Morling
 *
 */
public class ObjectIdJpaTest extends JpaTestCase {

	@Test
	public void canUseObjectIdAssignedUponInsertInOneToManyAssociation() throws Exception {
		EntityManager em = null;

		try {
			getTransactionManager().begin();
			em = getFactory().createEntityManager();
			// given
			Bar goldFishBar = new Bar( "Goldfisch Bar" );
			goldFishBar.getDoorMen().add( new DoorMan( "Bruce" ) );
			goldFishBar.getDoorMen().add( new DoorMan( "Dwain" ) );

			// when
			em.persist( goldFishBar );
			getTransactionManager().commit();
			em.clear();

			// then
			getTransactionManager().begin();
			Bar barLoaded = em.find( Bar.class, goldFishBar.getId() );
			assertThat( barLoaded.getDoorMen() ).onProperty( "name" ).containsOnly( "Bruce", "Dwain" );

			getTransactionManager().commit();
		}
		finally {
			if ( em != null ) {
				em.close();
			}
		}
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { Bar.class, DoorMan.class, MusicGenre.class };
	}

	@Override
	protected void refineInfo(GetterPersistenceUnitInfo info) {
		info.getProperties().put( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "false" );
	}
}
