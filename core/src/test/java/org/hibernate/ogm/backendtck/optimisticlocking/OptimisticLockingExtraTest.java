/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.optimisticlocking;

import static org.fest.assertions.Assertions.assertThat;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.impl.ForwardingGridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


/**
 * Test for detecting concurrent updates by dialects which support atomic find/update semantics or have their own
 * optimistic locking scheme.
 *
 * @author Gunnar Morling
 */
@SkipByGridDialect(
		value = { GridDialectType.CASSANDRA, GridDialectType.INFINISPAN_REMOTE },
		comment = "list - bag semantics unsupported (no primary key)"
)
public class OptimisticLockingExtraTest extends OgmTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Test
	public void updateToEmbeddedCollectionCausesVersionToBeIncreased() throws Throwable {
		Galaxy galaxy = persistGalaxy();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Galaxy entity = session.get( Galaxy.class, galaxy.getId() );
		entity.getStars().add( new Star( "Algol" ) );

		transaction.commit();
		session.clear();

		session = openSession();
		transaction = session.beginTransaction();

		entity = session.get( Galaxy.class, galaxy.getId() );
		assertThat( entity.getVersion() ).isEqualTo( 1 );
		assertThat( entity.getStars() ).hasSize( 3 );

		transaction.commit();
		session.close();
	}

	private Galaxy persistGalaxy() {
		Session session = openSession();

		session.beginTransaction();

		Galaxy milkyWay = new Galaxy( "galaxy-1", "Milky Way", new Star( "Sun" ), new Star( "Alpha Centauri" ) );
		session.persist( milkyWay );

		session.getTransaction().commit();
		session.close();

		return milkyWay;
	}

	@Override
	protected void configure(Map<String, Object> settings) {
		settings.put( OgmProperties.GRID_DIALECT, TestDialect.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Galaxy.class };
	}

	@SuppressWarnings("serial")
	public static class TestDialect extends ForwardingGridDialect<Serializable> {

		public TestDialect(DatastoreProvider provider) {
			super( TestHelper.getCurrentGridDialect( provider ) );
		}

		@Override
		public boolean updateTupleWithOptimisticLock(EntityKey entityKey, Tuple oldVersion, Tuple tuple, TupleContext tupleContext) {
			if ( Thread.currentThread().getName().equals( "ogm-test-thread-0" ) ) {
				waitALittleBit();
			}

			return super.updateTupleWithOptimisticLock( entityKey, oldVersion, tuple, tupleContext );
		}

		@Override
		public boolean removeTupleWithOptimisticLock(EntityKey entityKey, Tuple oldVersion, TupleContext tupleContext) {
			if ( Thread.currentThread().getName().equals( "ogm-test-thread-0" ) ) {
				waitALittleBit();
			}

			return super.removeTupleWithOptimisticLock( entityKey, oldVersion, tupleContext );
		}

		private void waitALittleBit() {
			try {
				Thread.sleep( 1000 );
			}
			catch (InterruptedException e) {
				throw new RuntimeException( e );
			}
		}
	}
}
