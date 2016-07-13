/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.transaction;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.datastore.neo4j.dialect.impl.NodeLabel.ENTITY;
import static org.hibernate.ogm.datastore.neo4j.test.dsl.GraphAssertions.node;
import static org.junit.Assert.fail;

import javax.persistence.EntityManager;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.mapping.Neo4jJpaTestCase;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.junit.Test;

/**
 * Test for using the compensation SPI with JPA.
 *
 * @author Gunnar Morling
 */
public class JtaRollbackTest extends Neo4jJpaTestCase {

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		info.setTransactionType( PersistenceUnitTransactionType.JTA );
	}

	@Test
	public void testRollbackCausedByException() throws Exception {
		final Game game1 = new Game( "game-1", "Title 1" );
		TransactionManager transactionManager = getTransactionManager( getFactory() );
		transactionManager.begin();
		EntityManager em = getFactory().createEntityManager();
		em.persist( game1 );

		em.flush();
		em.clear();
		transactionManager.commit();

		transactionManager.begin();
		em.joinTransaction();
		try {
			// This should generate an exception because we already have an entry with the same id in the db
			em.persist( new Game( game1.getId(), "New " + game1.getTitle() ) );
			transactionManager.commit();
			fail( "Expected exception was not raised" );
		}
		catch (Exception e) {
			// Entity already exists exception
			assertThat( e.getCause().getMessage() ).matches( ".*OGM000067.*" );
		}
		em.close();

		NodeForGraphAssertions gameNode1 = node( "g1", Game.class.getSimpleName(), ENTITY.name() )
				.property( "id", game1.getId() )
				.property( "title", game1.getTitle() );

		assertThatOnlyTheseNodesExist( gameNode1 );
	}

	@Test
	public void testManualRollback() throws Exception {
		final Game game1 = new Game( "game-1", "Title 1" );
		TransactionManager transactionManager = getTransactionManager( getFactory() );
		transactionManager.begin();
		EntityManager em = getFactory().createEntityManager();
		em.persist( game1 );

		em.flush();
		em.clear();
		transactionManager.commit();

		transactionManager.begin();
		em.joinTransaction();
		em.persist( new Game( "game-2", "Title 2" ) );
		transactionManager.rollback();
		em.close();

		NodeForGraphAssertions gameNode1 = node( "g1", Game.class.getSimpleName(), ENTITY.name() )
				.property( "id", game1.getId() )
				.property( "title", game1.getTitle() );

		assertThatOnlyTheseNodesExist( gameNode1 );
	}

	@Test
	// Neo4j is not an XAResource, and it will commit before the JTA transaction is committed.
	// If something goes wrong after the Neo4j transaction has been closed, it won't be possible to rollback.
	//
	// Note that this test might fails for two reasons:
	// 1) Neo4j participate correctly to the JTA transaction
	// 2) The failure occurs before Neo4j has synchronized
	//
	// If one of these situations occurs you will need to udpate the test
	public void testFailedRollback() throws Exception {
		final Game game1 = new Game( "game-1", "Title 1" );
		final Game game2 = new Game( "game-2", "Title 2" );
		TransactionManager transactionManager = getTransactionManager( getFactory() );
		transactionManager.begin();
		EntityManager em = getFactory().createEntityManager();
		em.persist( game1 );

		em.flush();
		em.clear();
		transactionManager.commit();

		transactionManager.begin();
		em.joinTransaction();
		Transaction transaction = transactionManager.getTransaction();
		transaction.registerSynchronization( new Synchronization() {

			@Override
			public void beforeCompletion() {
				throw new RuntimeException( "Don't panic; this is just a test" );
			}

			@Override
			public void afterCompletion(int status) {
			}
		} );

		em.persist( game2 );
		try {
			transactionManager.commit();
			fail( "Expected exception was not raised" );
		}
		catch (Exception e) {
			// Exception raise by a registered synchronisation
			assertThat( e.getCause().getMessage() ).matches( ".*Don't panic.*" );
		}
		em.close();

		NodeForGraphAssertions gameNode1 = node( "g1", Game.class.getSimpleName(), ENTITY.name() )
				.property( "id", game1.getId() )
				.property( "title", game1.getTitle() );

		NodeForGraphAssertions gameNode2 = node( "g2", Game.class.getSimpleName(), ENTITY.name() )
				.property( "id", game2.getId() )
				.property( "title", game2.getTitle() );

		assertThatOnlyTheseNodesExist( gameNode1, gameNode2 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Game.class };
	}
}
