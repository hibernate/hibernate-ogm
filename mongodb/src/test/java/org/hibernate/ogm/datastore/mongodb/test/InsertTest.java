/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.mongodb.test;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.ogm.datastore.mongodb.utils.MongoDbServerRunner;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author The Viet Nguyen
 */
@TestForIssue(jiraKey = "OGM-1222")
@RunWith(MongoDbServerRunner.class)
public class InsertTest extends OgmTestCase {

	/**
	 * Verify flush does not duplicate Entity on changing field, even if we are in
	 * the same transaction that creates the Entity itself.
	 */
	@Test
	public void testModifyObjectAfterPersisting() {
		inTransaction( em -> {
			Subject subject = new Subject( "1", "name" );
			em.persist( subject );
			subject.setName( "name2" );
			em.flush();
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Subject.class };
	}

	@Entity(name = "subject")
	static class Subject {

		@Id
		private String id;

		private String name;

		public Subject(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
