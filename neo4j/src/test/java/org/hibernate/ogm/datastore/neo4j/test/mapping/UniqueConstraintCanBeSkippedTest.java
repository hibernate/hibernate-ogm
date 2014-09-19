/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.mapping;

import static org.fest.assertions.Assertions.assertThat;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;

import org.hibernate.cfg.Environment;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.junit.Before;
import org.junit.Test;

/**
 * Test that the creation of unique constraints is skipped when
 * {@link Environment#UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY} is set to
 * {@link  UniqueConstraintSchemaUpdateStrategy#SKIP}
 *
 * @author Davide D'Alto
 */
public class UniqueConstraintCanBeSkippedTest extends Neo4jJpaTestCase {

	private EntityWithConstraints entityWithConstraints;

	@Entity
	static class EntityWithConstraints {
		@Id
		private String id;

		@Column(unique = true)
		private long uniqueColumn;

		public String getId() {
			return id;
		}

		public void setId(String login) {
			this.id = login;
		}

		public long getUniqueColumn() {
			return uniqueColumn;
		}

		public void setUniqueColumn(long insuranceNumber) {
			this.uniqueColumn = insuranceNumber;
		}
	}

	@Before
	public void setup() throws Exception {
		getTransactionManager().begin();
		final EntityManager em = getFactory().createEntityManager();

		entityWithConstraints = new EntityWithConstraints();
		entityWithConstraints.setId( "id_1" );
		entityWithConstraints.setUniqueColumn( 12345678 );

		em.persist( entityWithConstraints );
		commitOrRollback( true );
		em.close();
	}

	@Test
	public void skipUniqueConstraintsGenerationWhenRequired() throws Exception {
		{
			getTransactionManager().begin();
			final EntityManager em = getFactory().createEntityManager();
			EntityWithConstraints duplicated = new EntityWithConstraints();
			duplicated.setId( "id_2" );
			duplicated.setUniqueColumn( entityWithConstraints.getUniqueColumn() );
			em.persist( duplicated );
			getTransactionManager().commit();
			em.close();
		}
		{
			getTransactionManager().begin();
			final EntityManager em = getFactory().createEntityManager();
			EntityWithConstraints entity1 = em.find( EntityWithConstraints.class, "id_1" );
			EntityWithConstraints entity2 = em.find( EntityWithConstraints.class, "id_2" );
			assertThat( entity1.getUniqueColumn() ).isEqualTo( entity2.getUniqueColumn() );
			getTransactionManager().commit();
			em.close();
		}
	}

	@Override
	protected void refineInfo(GetterPersistenceUnitInfo info) {
		info.getProperties().put( Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, UniqueConstraintSchemaUpdateStrategy.SKIP );
	}

	@Override
	public Class<?>[] getEntities() {
		return new Class<?>[] { EntityWithConstraints.class };
	}
}
