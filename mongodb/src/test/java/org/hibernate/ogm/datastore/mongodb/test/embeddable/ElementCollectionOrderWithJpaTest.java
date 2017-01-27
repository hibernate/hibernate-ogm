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
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "OGM-1238")
public class ElementCollectionOrderWithJpaTest extends OgmJpaTestCase {

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
		EntityManager em = getFactory().createEntityManager();
		try {
			em.getTransaction().begin();
			PlainEntity entity = new PlainEntity();
			entity.id = ENTITY_ID;
			for ( Object event : EVENTS ) {
				entity.events.add( (String) event );
			}
			em.persist( entity );
			em.getTransaction().commit();
		}
		finally {
			em.close();
		}
	}

	@After
	public void after() {
		EntityManager em = getFactory().createEntityManager();
		try {
			em.getTransaction().begin();
			PlainEntity entity = em.find( PlainEntity.class, ENTITY_ID );
			if ( entity != null ) {
				em.remove( entity );
			}
			em.getTransaction().commit();
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testOrderWithFind() {
		EntityManager em = getFactory().createEntityManager();
		try {
			em.getTransaction().begin();
			PlainEntity entity = em.find( PlainEntity.class, ENTITY_ID );

			assertThat( entity ).isNotNull();
			assertThatCollectionIsCorrect( entity.events, EVENTS );
			em.getTransaction().commit();
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testOrderWithGetReference() {
		EntityManager em = getFactory().createEntityManager();
		try {
			em.getTransaction().begin();
			PlainEntity entity = em.getReference( PlainEntity.class, ENTITY_ID );

			assertThat( entity ).isNotNull();
			assertThatCollectionIsCorrect( entity.events, EVENTS );
			em.getTransaction().commit();
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testOrderWithQueryAndUniqueResult() {
		EntityManager em = getFactory().createEntityManager();
		try {
			em.getTransaction().begin();
			PlainEntity entity = (PlainEntity) em.createQuery( "from " + PlainEntity.class.getName() )
					.getSingleResult();

			assertThat( entity ).isNotNull();
			assertThatCollectionIsCorrect( entity.events, EVENTS );
			em.getTransaction().commit();
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testOrderWithQueryAndList() {
		EntityManager em = getFactory().createEntityManager();
		try {
			em.getTransaction().begin();
			PlainEntity entity = (PlainEntity) em.createQuery( "from " + PlainEntity.class.getName() )
					.getResultList().get( 0 );

			assertThat( entity ).isNotNull();
			assertThatCollectionIsCorrect( entity.events, EVENTS );
			em.getTransaction().commit();
		}
		finally {
			em.close();
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
