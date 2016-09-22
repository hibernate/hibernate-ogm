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

import org.hibernate.ogm.datastore.neo4j.test.dsl.NodeForGraphAssertions;
import org.hibernate.ogm.datastore.neo4j.test.mapping.Neo4jJpaTestCase;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.junit.Test;

/**
 * Test for using the compensation SPI with JPA.
 *
 * @author Gunnar Morling
 */
public class ResourceLocalRollbackTest extends Neo4jJpaTestCase {

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		info.setTransactionType( PersistenceUnitTransactionType.RESOURCE_LOCAL );
	}

	@Test
	public void testRollbackCausedByException() throws Exception {
		Game game1 = new Game( "game-1", "Title 1" );
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		em.persist( game1 );

		em.flush();
		em.clear();
		em.getTransaction().commit();

		em.getTransaction().begin();
		try {
			// This should generate an exception because we already have an entity with the same id in the db
			em.persist( new Game( game1.getId(), "New " + game1.getTitle() ) );
			em.getTransaction().commit();
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
		Game game1 = new Game( "game-1", "Title 1" );
		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();
		em.persist( game1 );

		em.flush();
		em.clear();
		em.getTransaction().commit();

		em.getTransaction().begin();
		em.persist( new Game( "game-2", "Title 2" ) );
		em.getTransaction().rollback();
		em.close();

		NodeForGraphAssertions gameNode1 = node( "g1", Game.class.getSimpleName(), ENTITY.name() )
				.property( "id", game1.getId() )
				.property( "title", game1.getTitle() );

		assertThatOnlyTheseNodesExist( gameNode1 );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Game.class };
	}
}
