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

public class PreRemoveTest extends OgmJpaTestCase {

	private EntityManager em;

	@Before
	public void setUp() {
		em = getFactory().createEntityManager();
	}

	@After
	public void tearDown() {
		em.close();
	}

	/**
	 * Delete an entity which uses a @PreRemove annotated method
	 * to set not persistent boolean field to 'true'.
	 */
	@Test
	public void testFieldSetInPreRemove() throws Exception {
		em.getTransaction().begin();
		PreRemovableBus bus = new PreRemovableBus();
		bus.setId( 1 );
		em.persist( bus );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		bus = em.find( PreRemovableBus.class, 1 );

		assertNotNull( bus );
		assertFalse( bus.isPreRemoveInvoked() );

		em.remove( bus );

		// @PreRemove executed before the entity manager remove
		// operation is actually executed or cascaded
		assertTrue( bus.isPreRemoveInvoked() );

		em.getTransaction().commit();
	}

	@Test
	public void testFieldSetInPreRemoveByListener() throws Exception {
		em.getTransaction().begin();
		PreRemovableBus bus = new PreRemovableBus();
		bus.setId( 1 );
		em.persist( bus );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
		bus = em.find( PreRemovableBus.class, 1 );

		assertNotNull( bus );
		assertFalse( bus.isPreRemoveInvokedByListener() );

		em.remove( bus );

		assertTrue( bus.isPreRemoveInvokedByListener() );

		em.getTransaction().commit();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { PreRemovableBus.class };
	}
}
