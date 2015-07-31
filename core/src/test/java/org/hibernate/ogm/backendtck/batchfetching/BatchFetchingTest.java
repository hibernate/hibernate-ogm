/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.batchfetching;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.fest.assertions.Assertions;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.multiget.spi.MultigetGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.utils.InvokedOperationsLoggingDialect;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.stat.Statistics;
import org.junit.Test;

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
		getOperationsLogger().reset();
		for ( Floor currentFloor : tower.getFloors() ) {
			// load proxies
			Object entity = session.load( Floor.class, currentFloor.getId() );
			Hibernate.initialize( entity );
			assertTrue( Hibernate.isInitialized( entity ) );
		}

		// if a multiget, we load both entities as one go, otherwise we don't
		int fetchSize = isMultigetDialect() ? 1 : 2;
		assertEquals( fetchSize, statistics.getEntityStatistics( Floor.class.getName() ).getFetchCount() );

		if ( isMultigetDialect() ) {
			assertThat( getOperations() ).containsExactly(
					"getTuples"
			);
		}
		else {
			assertThat( getOperations() ).containsExactly(
					"getTuple",
					"getTuple"
			);
		}

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
		tower = session.get( Tower.class, tower.getId() );

		Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.setStatisticsEnabled( true );
		statistics.clear();
		assertEquals( 0, statistics.getEntityStatistics( Floor.class.getName() ).getFetchCount() );
		getOperationsLogger().reset();
		Assertions.assertThat( tower.getFloors() ).hasSize( 2 );

		// if a multiget, we load both entities as one go, otherwise we don't
		int fetchSize = isMultigetDialect() ? 1 : 2;
		assertEquals( fetchSize, statistics.getEntityStatistics( Floor.class.getName() ).getFetchCount() );
		session.getTransaction().commit();

		if ( isMultigetDialect() ) {
			assertThat( getOperations() ).containsExactly(
					"getAssociation",
					"getTuples"
			);
		}
		else {
			assertThat( getOperations() ).containsExactly(
					"getAssociation",
					"getTuple",
					"getTuple"
			);
		}
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

	private boolean isMultigetDialect() {
		GridDialect gridDialect = sfi().getServiceRegistry().getService( GridDialect.class );
		return GridDialects.hasFacet( gridDialect, MultigetGridDialect.class );
	}

	@Override
	protected void configure(Map<String, Object> cfg) {
		cfg.put( OgmProperties.GRID_DIALECT, InvokedOperationsLoggingDialect.class );
	}

	private InvokedOperationsLoggingDialect getOperationsLogger() {
		GridDialect gridDialect = sfi().getServiceRegistry().getService( GridDialect.class );
		InvokedOperationsLoggingDialect invocationLogger = GridDialects.getDelegateOrNull(
				gridDialect,
				InvokedOperationsLoggingDialect.class
		);

		return invocationLogger;
	}

	private List<String> getOperations() {
		return getOperationsLogger().getOperations();
	}
}
