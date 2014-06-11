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

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

/**
 * Base class for OGM tests. While tests also can directly make use of {@link OgmTestRunner}, this base class provides
 * template methods for entity type configuration and modifications to {@link Configuration} as well as a member for the
 * session factory.
 *
 * @author Gunnar Morling
 */
@RunWith(OgmTestRunner.class)
public abstract class OgmTestCase {

	/**
	 * The session factory used by all test methods of a test class.
	 */
	@TestSessionFactory
	protected OgmSessionFactory sessions;

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

	@SessionFactoryConfiguration
	private void modifyConfiguration(Configuration cfg) {
		configure( cfg );
	}

	/**
	 * Can be overridden in subclasses to inspect or modify the {@link Configuration} of this test.
	 *
	 * @param cfg the configuration
	 */
	protected void configure(Configuration cfg) {
	}

	protected OgmSession openSession() {
		OgmSession session = sessions.openSession();
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
				if ( transaction != null && transaction.isActive() ) {
					transaction.rollback();
				}
				session.close();
			}
		}
	}

	protected SessionFactoryImplementor sfi() {
		return sessions;
	}

	protected void checkCleanCache() {
		assertThat( getNumberOfEntities( sessions ) ).as( "Entity cache should be empty" ).isEqualTo( 0 );
		assertThat( getNumberOfAssociations( sessions ) ).as( "Association cache should be empty" ).isEqualTo( 0 );
	}
}
