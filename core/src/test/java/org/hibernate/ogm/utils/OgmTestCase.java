/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfAssociations;
import static org.hibernate.ogm.utils.TestHelper.getNumberOfEntities;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.engine.spi.OgmSessionFactoryImplementor;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * Base class for OGM tests. While tests also can directly make use of {@link OgmTestRunner}, this base class provides
 * template methods for entity type configuration and modifications to the configuration as well as a member for the
 * session factory.
 *
 * @author Gunnar Morling
 * @author Fabio Massimo Ercoli
 */
@RunWith(OgmTestRunner.class)
public abstract class OgmTestCase {

	/**
	 * The session factory used by all test methods of a test class.
	 */
	@TestSessionFactory
	protected OgmSessionFactory sessionFactory;

	private List<Session> openedSessions;

	@TestEntities
	private Class<?>[] getTestEntities() {
		return getAnnotatedClasses();
	}

	/**
	 * Must be implemented by subclasses to return the entity types used by this test.
	 *
	 * @return an array with this tests entity types
	 */
	protected abstract Class<?>[] getAnnotatedClasses();

	@TestSessionFactoryConfiguration
	private void modifyConfiguration(Map<String, Object> cfg) {
		configure( cfg );
	}

	/**
	 * Can be overridden in subclasses to inspect or modify the configuration of this test.
	 *
	 * @param cfg the configuration
	 */
	protected void configure(Map<String, Object> cfg) {
	}

	protected OgmSession openSession() {
		OgmSession session = sessionFactory.openSession();
		openedSessions.add( session );
		return session;
	}

	@Before
	public void setUp() {
		openedSessions = new ArrayList<Session>();
	}

	/**
	 * Closes all sessions opened via {@link #openSession()} which are still open and rolls back their transaction if it
	 * is still open.
	 */
	@After
	public void closeOpenedSessions() {
		for ( Session session : openedSessions ) {
			if ( session.isOpen() ) {
				Transaction transaction = session.getTransaction();
				if ( transaction != null && transaction.getStatus() == TransactionStatus.ACTIVE ) {
					transaction.rollback();
				}
				session.close();
			}
		}
	}

	protected OgmSessionFactoryImplementor getSessionFactory() {
		return (OgmSessionFactoryImplementor) sessionFactory;
	}

	protected void checkCleanCache() {
		assertThat( getNumberOfEntities( sessionFactory ) ).as( "Entity cache should be empty" ).isEqualTo( 0 );
		assertThat( getNumberOfAssociations( sessionFactory ) ).as( "Association cache should be empty" ).isEqualTo( 0 );
	}

	public void inTransaction(Consumer<Session> consumer) {
		try ( OgmSession session = openSession() ) {
			Transaction transaction = session.beginTransaction();

			try {
				consumer.accept( session );
				transaction.commit();
			}
			catch (Throwable t) {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
				throw t;
			}
		}
	}

}
