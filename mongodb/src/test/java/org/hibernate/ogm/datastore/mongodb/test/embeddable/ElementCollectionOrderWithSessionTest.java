/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.embeddable;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "OGM-1238")
public class ElementCollectionOrderWithSessionTest extends OgmTestCase {

	private static final String ENTITY_ID = "Entity";
	private static final String[] EVENTS = {
			"event 5",
			"event 3",
			"event 1",
			"event 2",
			"event 4",
			"event 14",
			"event x"
	};

	@Before
	public void before() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			PlainEntity entity = new PlainEntity();
			entity.id = ENTITY_ID;
			for ( Object event : EVENTS ) {
				entity.events.add( (String) event );
			}
			session.persist( entity );
			tx.commit();
		}
	}

	@After
	public void after() {
		try ( OgmSession session = openSession() ) {
			session.beginTransaction();
			session.delete( session.get( PlainEntity.class, ENTITY_ID ) );
			session.getTransaction().commit();
		}
	}

	@Test
	public void testOrderWithGet() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			PlainEntity entity = session.get( PlainEntity.class, ENTITY_ID );

			assertThat( entity ).isNotNull();
			assertThatCollectionIsCorrect( entity.events, EVENTS );
			tx.commit();
		}
	}

	@Test
	public void testOrderWithLoad() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			PlainEntity entity = session.load( PlainEntity.class, ENTITY_ID );

			assertThat( entity ).isNotNull();
			assertThatCollectionIsCorrect( entity.events, EVENTS );
			tx.commit();
		}
	}

	@Test
	public void testOrderWithQueryAndUniqueResult() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			PlainEntity entity = (PlainEntity) session.createQuery( "from " + PlainEntity.class.getName() ).uniqueResult();

			assertThat( entity ).isNotNull();
			assertThatCollectionIsCorrect( entity.events, EVENTS );
			tx.commit();
		}
	}

	@Test
	public void testOrderWithQueryAndList() {
		try ( OgmSession session = openSession() ) {
			Transaction tx = session.beginTransaction();
			PlainEntity entity = (PlainEntity) session.createQuery( "from " + PlainEntity.class.getName() ).list().get( 0 );

			assertThat( entity ).isNotNull();
			assertThatCollectionIsCorrect( entity.events, EVENTS );
			tx.commit();
		}
	}

	private void assertThatCollectionIsCorrect(List<String> actual, String[] expected) {
		int i = 0;
		for ( String entry : actual ) {
			assertThat( entry ).isEqualTo( expected[i++] );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ PlainEntity.class };
	}

	@Entity
	@Table(name = "PlainEntity")
	static class PlainEntity {

		@Id
		String id;

		@ElementCollection
		@CollectionTable(name = "events")
		List<String> events = new ArrayList<>();
	}
}
