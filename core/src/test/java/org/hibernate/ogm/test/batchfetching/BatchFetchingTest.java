/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.batchfetching;

import org.fest.assertions.Assertions;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.junit.Test;

import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.stat.Statistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class BatchFetchingTest extends OgmTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Tower.class, Floor.class};
	}

	@Test
	public void testLoadSeveralFloorsByBatch() throws Exception {
		Session session = openSession();
		Tower tower = prepareDataset( session );
		session.clear();

		session.beginTransaction();
		for ( Floor currentFloor : tower.getFloors() ) {
			// load proxies
			assertFalse( Hibernate.isInitialized( session.load( Floor.class, currentFloor.getId() ) ) );
		}
		Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.setStatisticsEnabled( true );
		statistics.clear();

		assertEquals( 0, statistics.getEntityStatistics( Floor.class.getName() ).getFetchCount() );
		for ( Floor currentFloor : tower.getFloors() ) {
			// load proxies
			Object entity = session.load( Floor.class, currentFloor.getId() );
			Hibernate.initialize( entity );
			assertTrue( Hibernate.isInitialized( entity ) );
		}
		assertEquals( 1, statistics.getEntityStatistics( Floor.class.getName() ).getFetchCount() );

		session.getTransaction().commit();

		cleanDataset( session, tower );
		session.close();

	}

	@Test
	public void testLoadSeveralFloorsFromTower() throws Exception {

		Session session = openSession();
		Tower tower = prepareDataset( session );
		session.clear();

		// now read the tower and its floors to detect 1+n patterns;
		session.beginTransaction();
		tower = (Tower) session.get( Tower.class, tower.getId() );

		Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.setStatisticsEnabled( true );
		statistics.clear();
		assertEquals( 0, statistics.getEntityStatistics( Floor.class.getName() ).getFetchCount() );
		Assertions.assertThat( tower.getFloors() ).hasSize( 2 );
		assertEquals( 1, statistics.getEntityStatistics( Floor.class.getName() ).getFetchCount() );
		session.getTransaction().commit();

		cleanDataset( session, tower );
		session.close();

	}

	private void cleanDataset(Session session, Tower tower) {
		session.beginTransaction();
		session.delete( session.get( Tower.class, tower.getId() ) );
		for ( Floor currentFloor : tower.getFloors() ) {
			session.delete( session.get( Floor.class, currentFloor.getId() ) );
		}
		session.getTransaction().commit();
	}

	private Tower prepareDataset(Session session) {
		session.beginTransaction();
		Tower tower = new Tower();
		tower.setName( "Pise" );
		Floor floor = new Floor();
		floor.setLevel( 0 );
		tower.getFloors().add( floor );
		floor = new Floor();
		floor.setLevel( 1 );
		tower.getFloors().add( floor );
		session.persist( tower );
		session.getTransaction().commit();
		return tower;
	}
}
