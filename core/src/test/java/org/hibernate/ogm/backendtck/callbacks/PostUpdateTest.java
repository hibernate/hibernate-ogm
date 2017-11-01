/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.callbacks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.HibernateException;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
		try {
			inTx( em, ( EntityManager em ) -> em.remove( em.find( PostUpdatableBus.class, 1 ) ) );
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testFieldSetInPostUpdateAnnotation() {
		// Persist
		inTx( em, ( EntityManager em ) -> em.persist( new PostUpdatableBus( 1, INITIAL ) ) );

		// Update
		Function<EntityManager, PostUpdatableBus> function = new Function<EntityManager, PostUpdatableBus>() {

			@Override
			public PostUpdatableBus apply(EntityManager t) {
				PostUpdatableBus bus = em.find( PostUpdatableBus.class, 1 );
				assertEquals( bus.getField(), INITIAL );
				assertFalse( bus.isPostUpdated() );
				bus.setField( UPDATED );
				return bus;
			}
		};

		PostUpdatableBus postUpdatedBus = inTx( em, function );
		assertTrue( postUpdatedBus.isPostUpdated() );
	}

	@Test
	public void testFieldSetInPostUpdateByListener() {
		// Persist
		inTx( em, ( EntityManager em ) -> em.persist( new PostUpdatableBus( 1, INITIAL ) ) );

		// Update
		Function<EntityManager, PostUpdatableBus> function = new Function<EntityManager, PostUpdatableBus>() {

			@Override
			public PostUpdatableBus apply(EntityManager t) {
				PostUpdatableBus bus = em.find( PostUpdatableBus.class, 1 );
				assertEquals( bus.getField(), INITIAL );
				assertFalse( bus.isPostUpdatedByListener() );
				bus.setField( UPDATED );
				return bus;
			}
		};

		PostUpdatableBus postUpdatedBus = inTx( em, function );
		assertTrue( postUpdatedBus.isPostUpdatedByListener() );
	}

	public static <T extends Bus> T inTx(EntityManager em, Function<EntityManager, T> consumer) {
		EntityTransaction tx = em.getTransaction();
		try {
			tx.begin();
			T bus = consumer.apply( em );
			tx.commit();
			em.clear();
			return bus;
		}
		catch (HibernateException hex) {
			if ( tx != null && tx.isActive() ) {
				tx.rollback();
			}
			throw hex;
		}
	}

	public static void inTx(EntityManager em, Consumer<EntityManager> consumer) {
		EntityTransaction tx = em.getTransaction();
		try {
			tx.begin();
			consumer.accept( em );
			tx.commit();
			em.clear();
		}
		catch (HibernateException hex) {
			if ( tx != null && tx.isActive() ) {
				tx.rollback();
			}
			throw hex;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ PostUpdatableBus.class };
	}
}
