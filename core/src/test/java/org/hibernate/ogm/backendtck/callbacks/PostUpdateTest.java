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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PostUpdateTest extends OgmJpaTestCase {

	private static final String INITIAL = "initial";
	private static final String UPDATED = "updated";

	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	@Override
	public void removeEntities() throws Exception {
		em.getTransaction().begin();
		em.remove( em.find( PostUpdatableBus.class, 1 ) );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Update an entity which uses a @PostUpdate annotated method
	 * to set boolean field to 'true'.
	 */
	@Test
	public void testFieldSetInPostUpdate() {
		em.getTransaction().begin();

		PostUpdatableBus bus = new PostUpdatableBus();
		bus.setId( 1 );
		bus.setField( INITIAL );

		em.persist( bus );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		bus = em.find( PostUpdatableBus.class, bus.getId() );

		assertNotNull( bus );
		assertEquals( bus.getField(), INITIAL );
		assertFalse( bus.isPostUpdated() );
		bus.setField( UPDATED );

		em.getTransaction().commit();

		assertTrue( bus.isPostUpdated() );
		em.clear();

		em.getTransaction().begin();

		bus = em.find( PostUpdatableBus.class, bus.getId() );
		assertNotNull( bus );
		assertEquals( bus.getField(), UPDATED );
		// @PostUpdate executed after the database UPDATE operation
		assertFalse( bus.isPostUpdated() );

		em.getTransaction().commit();
	}

	@Test
	public void testFieldSetInPostUpdateByListener() {
		em.getTransaction().begin();

		PostUpdatableBus bus = new PostUpdatableBus();
		bus.setId( 1 );
		bus.setField( INITIAL );

		em.persist( bus );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		bus = em.find( PostUpdatableBus.class, bus.getId() );

		assertNotNull( bus );
		assertEquals( bus.getField(), INITIAL );
		assertFalse( bus.isPostUpdatedByListener() );

		bus.setField( UPDATED );
		em.getTransaction().commit();

		assertTrue( bus.isPostUpdatedByListener() );

		em.clear();

		em.getTransaction().begin();

		bus = em.find( PostUpdatableBus.class, bus.getId() );
		assertNotNull( bus );
		assertEquals( bus.getField(), UPDATED );
		assertFalse( bus.isPostUpdatedByListener() );

		em.getTransaction().commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PostUpdatableBus.class };
	}
}
