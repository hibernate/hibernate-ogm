/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.CASSANDRA;
import static org.hibernate.ogm.utils.GridDialectType.COUCHDB;
import static org.hibernate.ogm.utils.GridDialectType.EHCACHE;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN_REMOTE;
import static org.hibernate.ogm.utils.GridDialectType.REDIS_HASH;
import static org.hibernate.ogm.utils.GridDialectType.REDIS_JSON;

import javax.persistence.EntityManager;

import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
@SkipByGridDialect(
		value = { CASSANDRA, COUCHDB, EHCACHE, HASHMAP, INFINISPAN, INFINISPAN_REMOTE, REDIS_JSON, REDIS_HASH },
		comment = "We need a QueryParserService to be able to perform these queries.")
public class QueriesWithToOnePropertyTest extends OgmJpaTestCase {

	private EntityManager em;

	@Test
	@TestForIssue(jiraKey = "OGM-854")
	public void testJPQL() throws Exception {
		Author alma = (Author) em.createQuery( "FROM Author WHERE name = :name" )
				.setParameter( "name", "alma" )
				.getSingleResult();

		assertThat( alma.getAddress() ).isNotNull();
	}

	@Before
	public void populateDb() throws Exception {
		em = getFactory().createEntityManager();
		em.getTransaction().begin();

		Address mainStreet = new Address();
		mainStreet.setId( 1L );
		mainStreet.setCity( "London" );
		mainStreet.setStreet( "Main Street" );
		em.persist( mainStreet );

		Author alma = new Author();
		alma.setId( 2L );
		alma.setName( "alma" );
		alma.setAddress( mainStreet );
		em.persist( alma );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
	}

	@After
	public void closeEmAndRemoveEntities() throws Exception {
		//Do not hide the real cause with an NPE if there are initialization issues:
		if ( em != null ) {
			em.getTransaction().commit();
			em.close();
			removeEntities();
		}
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Author.class, Address.class, Hypothesis.class };
	}

}
