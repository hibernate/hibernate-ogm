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
import static org.junit.Assert.assertEquals;

public class PreUpdateTest extends OgmJpaTestCase {

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
		em.remove( em.find( PreUpdatableBus.class, 1 ) );
		em.getTransaction().commit();
		em.close();
	}

	/**
	 * Update an entity which uses a @PreUpdate annotated method
	 * to set boolean field to 'true'.
	 */
	@Test
	public void testFieldSetInPreUpdate() {
		em.getTransaction().begin();

		PreUpdatableBus bus = new PreUpdatableBus();
		bus.setId( 1 );
		bus.setField( INITIAL );

		em.persist( bus );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		bus = em.find( PreUpdatableBus.class, bus.getId() );

		assertNotNull( bus );
		assertEquals( bus.getField(), INITIAL );
		assertFalse( bus.isPreUpdated() );

		bus.setField( UPDATED );
		em.getTransaction().commit();

		assertTrue( bus.isPreUpdated() );

		em.clear();

		assertTrue( bus.isPreUpdated() );

		em.getTransaction().begin();

		bus = em.find( PreUpdatableBus.class, bus.getId() );
		assertNotNull( bus );
		assertEquals( bus.getField(), UPDATED );
		// @PreUpdate executed before the database UPDATE operation
		assertTrue( bus.isPreUpdated() );

		em.getTransaction().commit();
	}

	@Test
	public void testFieldSetInPreUpdateByListener() {
		em.getTransaction().begin();

		PreUpdatableBus bus = new PreUpdatableBus();
		bus.setId( 1 );
		bus.setField( INITIAL );

		em.persist( bus );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		bus = em.find( PreUpdatableBus.class, bus.getId() );

		assertNotNull( bus );
		assertEquals( bus.getField(), INITIAL );
		assertFalse( bus.isPreUpdatedByListener() );

		bus.setField( UPDATED );
		em.getTransaction().commit();

		assertTrue( bus.isPreUpdatedByListener() );

		em.clear();

		assertTrue( bus.isPreUpdatedByListener() );

		em.getTransaction().begin();

		bus = em.find( PreUpdatableBus.class, bus.getId() );
		assertNotNull( bus );
		assertEquals( bus.getField(), UPDATED );
		assertTrue( bus.isPreUpdatedByListener() );

		em.getTransaction().commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PreUpdatableBus.class };
	}
}
