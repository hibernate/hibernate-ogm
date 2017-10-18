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

public class PostPersistTest extends OgmJpaTestCase {

	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	@Override
	public void removeEntities() throws Exception {
		em.getTransaction().begin();
		em.remove( em.find( PostPersistableBus.class, 1 ) );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Insert an entity which uses a @PostPersist annotated method
	 * to set not persistent boolean field to 'true'.
	 */
	@Test
	public void testFieldSetInPostPersist() throws Exception {
		em.getTransaction().begin();

		PostPersistableBus bus = new PostPersistableBus();
		bus.setId( 1 );

		assertFalse( bus.isPostPersisted() );

		em.persist( bus );

		assertFalse( bus.isPostPersisted() );

		em.getTransaction().commit();

		// @PostPersist executed before the entity manager persist
		// operation is actually executed or cascaded
		assertTrue( bus.isPostPersisted() );

		em.clear();

		em.getTransaction().begin();
		bus = em.find( PostPersistableBus.class, bus.getId() );

		assertNotNull( bus );

		em.getTransaction().commit();
	}

	@Test
	public void testFieldSetInPostPersistByListener() throws Exception {
		em.getTransaction().begin();

		PostPersistableBus bus = new PostPersistableBus();
		bus.setId( 1 );

		assertFalse( bus.isPostPersistedByListener() );

		em.persist( bus );

		assertFalse( bus.isPostPersistedByListener() );

		em.getTransaction().commit();

		assertTrue( bus.isPostPersistedByListener() );

		em.clear();

		em.getTransaction().begin();
		bus = em.find( PostPersistableBus.class, bus.getId() );

		assertNotNull( bus );

		em.getTransaction().commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PostPersistableBus.class };
	}
}
