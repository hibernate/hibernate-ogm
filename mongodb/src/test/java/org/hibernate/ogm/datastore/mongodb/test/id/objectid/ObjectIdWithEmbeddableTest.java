/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.objectid;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.backendtck.queries.AnEmbeddable;
import org.hibernate.ogm.backendtck.queries.AnotherEmbeddable;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Test;

/**
 * Tests for storing entities with object ids and embeddables in MongoDB.
 *
 * @author Davide D'Alto
 */
public class ObjectIdWithEmbeddableTest extends OgmTestCase {

	@Test
	@TestForIssue(jiraKey = "OGM-612")
	public void canUseObjectIdAssignedUponInsertWithEmbeddable() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		EntityWithObjectIdAndEmbeddable entity = new EntityWithObjectIdAndEmbeddable();
		AnEmbeddable anEmbeddable = new AnEmbeddable( "a very nice string", null );
		entity.setAnEmbeddable( anEmbeddable );

		// when
		session.persist( entity );

		tx.commit();
		session.clear();
		tx = session.beginTransaction();

		EntityWithObjectIdAndEmbeddable loaded = (EntityWithObjectIdAndEmbeddable) session.load( EntityWithObjectIdAndEmbeddable.class, entity.getId() );

		// then
		assertThat( loaded.getId() ).isEqualTo( entity.getId() );
		assertThat( loaded.getAnEmbeddable().getEmbeddedString() ).isEqualTo( entity.getAnEmbeddable().getEmbeddedString() );
		assertThat( loaded.getAnEmbeddable().getAnotherEmbeddable() ).isEqualTo( entity.getAnEmbeddable().getAnotherEmbeddable() );

		tx.commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-612")
	public void canUseObjectIdAssignedUponInsertWithNestedEmbeddable() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		EntityWithObjectIdAndEmbeddable entity = new EntityWithObjectIdAndEmbeddable();
		AnotherEmbeddable anotherEmbeddable = new AnotherEmbeddable( "Another nice string ... nested" );
		AnEmbeddable anEmbeddable = new AnEmbeddable( "a very nice string", anotherEmbeddable );
		entity.setAnEmbeddable( anEmbeddable );

		// when
		session.persist( entity );

		tx.commit();
		session.clear();
		tx = session.beginTransaction();

		EntityWithObjectIdAndEmbeddable loaded = (EntityWithObjectIdAndEmbeddable) session.load( EntityWithObjectIdAndEmbeddable.class, entity.getId() );

		// then
		assertThat( loaded.getId() ).isEqualTo( entity.getId() );
		assertThat( loaded.getAnEmbeddable().getEmbeddedString() ).isEqualTo( entity.getAnEmbeddable().getEmbeddedString() );
		assertThat( loaded.getAnEmbeddable().getAnotherEmbeddable().getEmbeddedString() ).isEqualTo(
				entity.getAnEmbeddable().getAnotherEmbeddable().getEmbeddedString() );

		tx.commit();
		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityWithObjectIdAndEmbeddable.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "false" );
	}
}
