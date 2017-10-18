/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.callbacks;

import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class PrePersistTest extends OgmJpaTestCase {

	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	@Override
	public void removeEntities() throws Exception {
		em.getTransaction().begin();
		em.remove( em.find( PrePersistableBus.class, 1 ) );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Insert an entity which uses a @PrePersist annotated method
	 * to set not persistent boolean field to 'true'.
	 */
	@Test
	public void testFieldSetInPrePersist() throws Exception {
		em.getTransaction().begin();

		PrePersistableBus bus = new PrePersistableBus();
		bus.setId( 1 );

		assertFalse( bus.isPrePersisted() );

		em.persist( bus );

		// @PrePersist executed before the entity manager persist
		// operation is actually executed or cascaded
		assertTrue( bus.isPrePersisted() );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		bus = em.find( PrePersistableBus.class, bus.getId() );

		assertNotNull( bus );

		em.getTransaction().commit();
	}

	@Test
	public void testFieldSetInPrePersistByListener() throws Exception {
		em.getTransaction().begin();

		PrePersistableBus bus = new PrePersistableBus();
		bus.setId( 1 );

		assertFalse( bus.isPrePersistedByListener() );

		em.persist( bus );

		assertTrue( bus.isPrePersistedByListener() );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		bus = em.find( PrePersistableBus.class, bus.getId() );

		assertNotNull( bus );

		em.getTransaction().commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PrePersistableBus.class };
	}
}
