/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test;

import java.util.ArrayList;
import java.util.List;

import org.fest.assertions.Assertions;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.backendtck.simpleentity.Helicopter;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.mongodb.MongoDBDialect;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.dialect.batch.spi.BatchableGridDialect;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.After;
import org.junit.Test;

/**
 * Test that the expected number of operations are queued during a flush event
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class BatchInsertTest extends OgmTestCase {

	@Test
	public void testImplicitFlushWithInserts() throws Exception {
		int numInsert = 3;

		Session session = openSession();
		session.beginTransaction();
		for ( int i = 0; i < numInsert; i++ ) {
			session.persist( helicopter( "H" + i ) );
		}
		session.getTransaction().commit();
		session.close();

		Assertions.assertThat( LeakingMongoDBDialect.queueSize ).isEqualTo( numInsert );
	}

	@Test
	public void testImplicitFlushWithUpdates() throws Exception {
		int numInsert = 3;
		int numUpdate = 1;

		Session session = openSession();
		session.beginTransaction();
		Helicopter helicopter = helicopter( "H_tmp" );
		session.persist( helicopter );
		for ( int i = 0; i < numInsert; i++ ) {
			session.persist( helicopter( "H_" + i ) );
		}
		helicopter.setName( "H_" + numInsert );
		session.getTransaction().commit();
		session.close();

		Assertions.assertThat( LeakingMongoDBDialect.queueSize ).isEqualTo( numUpdate + ( numInsert + 1 ) );
	}

	@Test
	public void testImplicitFlushWithDeletes() throws Exception {
		int numDelete = 3;

		Session session = openSession();
		session.beginTransaction();
		for ( int i = 0; i < numDelete; i++ ) {
			session.persist( helicopter( "H_" + i ) );
		}
		session.getTransaction().commit();
		session.close();

		LeakingMongoDBDialect.queueSize = -1;

		session = openSession();
		session.beginTransaction();
		List<Helicopter> helicopters = session.createQuery( "FROM Helicopter" ).list();
		for ( Helicopter helicopter : helicopters ) {
			session.delete( helicopter );
		}
		session.getTransaction().commit();
		session.close();

		Assertions.assertThat( LeakingMongoDBDialect.queueSize ).isEqualTo( numDelete );
	}

	@Test
	public void testImplicitFlushWithInsertsAndDelete() throws Exception {
		int numInsert = 3;

		Session session = openSession();
		session.beginTransaction();
		List<Helicopter> helicopters = new ArrayList<Helicopter>();
		for ( int i = 0; i < numInsert; i++ ) {
			Helicopter helicopter = helicopter( "H_" + i );
			session.persist( helicopter );
			helicopters.add( helicopter );
		}
		for ( Helicopter helicopter : helicopters ) {
			session.delete( helicopter );
		}
		session.getTransaction().commit();
		session.close();

		Assertions.assertThat( LeakingMongoDBDialect.queueSize ).isEqualTo( 2 * numInsert );
	}

	@After
	public void clean() {
		Session session = openSession();
		session.beginTransaction();
		List<Helicopter> helicopters = session.createQuery( "FROM Helicopter" ).list();
		for ( Helicopter helicopter : helicopters ) {
			session.delete( helicopter );
		}
		session.getTransaction().commit();
		session.close();
	}

	private Helicopter helicopter(String name) {
		Helicopter helicopter = new Helicopter();
		helicopter.setName( name );
		return helicopter;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Helicopter.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( OgmProperties.GRID_DIALECT, LeakingMongoDBDialect.class.getName() );
	}

	public static class LeakingMongoDBDialect extends MongoDBDialect implements BatchableGridDialect {

		static volatile int queueSize = 0;

		public LeakingMongoDBDialect(MongoDBDatastoreProvider provider) {
			super( provider );
		}

		@Override
		public void executeBatch(OperationsQueue queue) {
			queueSize = queue.size();
			super.executeBatch( queue );
		}

	}

}
