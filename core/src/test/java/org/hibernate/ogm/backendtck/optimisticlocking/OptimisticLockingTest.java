/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.optimisticlocking;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.isA;
import static org.hibernate.ogm.utils.GridDialectType.COUCHDB;
import static org.hibernate.ogm.utils.GridDialectType.EHCACHE;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J;

import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.impl.ForwardingGridDialect;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;
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
public class OptimisticLockingTest extends OgmTestCase {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	@SkipByGridDialect(
			value = { HASHMAP, INFINISPAN, EHCACHE, NEO4J, COUCHDB },
			comment = "Note that CouchDB has its own optimistic locking scheme, handled by the dialect itself."
	)
	public void updatingEntityUsingOldVersionCausesException() throws Throwable {
		thrown.expectCause( isA( StaleObjectStateException.class ) );

		persistPlanet();

		// for the first update, the test dialect waits a bit between read and write, so the second update will take
		// place in between, causing the exception
		Future<?> future1 = updatePlanetInSeparateThread( "Mars" );
		Future<?> future2 = updatePlanetInSeparateThread( "Uranus" );

		try {
			future2.get();
			future1.get();
		}
		finally {
			removePlanet();
		}
	}

	@Test
	public void updateToEmbeddedCollectionCausesVersionToBeIncreased() throws Throwable {
		Galaxy galaxy = persistGalaxy();

		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Galaxy entity = (Galaxy) session.get( Galaxy.class, galaxy.getId() );
		entity.getStars().add( new Star( "Algol" ) );

		transaction.commit();
		session.clear();

		session = openSession();
		transaction = session.beginTransaction();

		entity = (Galaxy) session.get( Galaxy.class, galaxy.getId() );
		assertThat( entity.getVersion() ).isEqualTo( 1 );
		assertThat( entity.getStars() ).hasSize( 3 );

		transaction.commit();
		session.clear();
	}

	private Future<?> updatePlanetInSeparateThread(final String newName) throws Exception {
		return Executors.newSingleThreadExecutor().submit( new Runnable() {

			@Override
			public void run() {
				Session session = openSession();
				Transaction transaction = session.beginTransaction();

				// load the entity and update it
				Planet entity = (Planet) session.get( Planet.class, "planet-1" );
				entity.setName( newName );

				transaction.commit();
				session.close();
			}
		} );
	}

	private Planet persistPlanet() {
		Session session = openSession();

		session.beginTransaction();

		Planet planet = new Planet( "planet-1", "Pluto" );
		session.persist( planet );

		session.getTransaction().commit();
		session.close();

		return planet;
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

	public void removePlanet() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();

		Planet entity = (Planet) session.get( Planet.class, "planet-1" );
		session.delete( entity );

		transaction.commit();
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.getProperties().put( OgmProperties.GRID_DIALECT, TestDialect.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Planet.class, Galaxy.class };
	}

	public static class TestDialect extends ForwardingGridDialect<Serializable> {

		public TestDialect(DatastoreProvider provider) {
			super( TestHelper.getCurrentGridDialect( provider ) );
		}

		@Override
		public boolean updateTuple(EntityKey entityKey, Tuple oldVersion, Tuple tuple, TupleContext tupleContext) {
			if ( Thread.currentThread().getName().equals( "ogm-test-thread-0" ) ) {
				waitALittleBit();
			}

			return super.updateTuple( entityKey, oldVersion, tuple, tupleContext );
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
