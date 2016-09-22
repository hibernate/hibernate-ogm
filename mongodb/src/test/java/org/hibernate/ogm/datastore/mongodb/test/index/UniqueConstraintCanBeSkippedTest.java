/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.index;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Transaction;
import org.hibernate.cfg.Environment;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.tool.hbm2ddl.UniqueConstraintSchemaUpdateStrategy;
import org.junit.Test;

/**
 * Test that the creation of unique constraints is skipped when
 * {@link Environment#UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY} is set to
 * {@link  UniqueConstraintSchemaUpdateStrategy#SKIP}
 *
 * @author Davide D'Alto
 * @author Guillaume Smet
 */
public class UniqueConstraintCanBeSkippedTest extends OgmTestCase {

	private static final long UNIQUE_COLUMN_VALUE = 12345678l;

	@Entity
	@Table(name = "EntityWithConstraints")
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

	@Test
	public void skipUniqueConstraintsGenerationWhenRequired() throws Exception {
		OgmSession session = openSession();

		{
			Transaction tx = session.beginTransaction();
			EntityWithConstraints duplicated = new EntityWithConstraints();
			duplicated.setId( "id_1" );
			duplicated.setUniqueColumn( UNIQUE_COLUMN_VALUE );
			session.persist( duplicated );
			tx.commit();
			session.clear();
		}

		{
			Transaction tx = session.beginTransaction();
			EntityWithConstraints duplicated = new EntityWithConstraints();
			duplicated.setId( "id_2" );
			duplicated.setUniqueColumn( UNIQUE_COLUMN_VALUE );
			session.persist( duplicated );
			tx.commit();
			session.clear();
		}

		Transaction tx = session.beginTransaction();
		EntityWithConstraints entity1 = session.load( EntityWithConstraints.class, "id_1" );
		EntityWithConstraints entity2 = session.load( EntityWithConstraints.class, "id_2" );
		assertThat( entity1.getUniqueColumn() ).isEqualTo( entity2.getUniqueColumn() );
		tx.commit();
		session.close();
	}

	@Override
	protected void configure(Map<String, Object> cfg) {
		cfg.put( Environment.UNIQUE_CONSTRAINT_SCHEMA_UPDATE_STRATEGY, UniqueConstraintSchemaUpdateStrategy.SKIP );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityWithConstraints.class };
	}
}
