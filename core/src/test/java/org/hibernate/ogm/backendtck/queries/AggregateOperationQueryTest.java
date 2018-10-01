/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN_REMOTE;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;
import static org.hibernate.ogm.utils.OgmAssertions.assertThat;

import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;

/**
 * @author Aleksandr Mylnikov
 */
@SkipByGridDialect(
		value = { HASHMAP, INFINISPAN, INFINISPAN_REMOTE, NEO4J_EMBEDDED, NEO4J_REMOTE },
		comment = "We need to improve hql parser for dialects")
public class AggregateOperationQueryTest extends OgmJpaTestCase {

	private EntityManager em;

	@Test
	public void shouldCountEntities() {
		Long result = (Long) em.createQuery( "SELECT COUNT(*) FROM Author author" ).getSingleResult();
		assertThat( result ).isEqualTo( 2l );
	}

	@Test
	public void shouldCountEntitiesWithCondition() {
		Long result = (Long) em.createQuery( "select count(*) from Author a WHERE id = :id" ).setParameter( "id", 1l ).getSingleResult();
		assertThat( result ).isEqualTo( 1l );
	}

	@Before
	public void populateDb() throws Exception {
		em = getFactory().createEntityManager();
		em.getTransaction().begin();
		em.persist( new Author( 1l, "Josh", null,  null ) );
		em.persist( new Author( 2l, "Ela", null, null ) );
		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();
	}

	@After
	public void closeEmAndRemoveEntities() throws Exception {
		//Do not hide the real cause with an NPE if there are initialization issues:
		if ( em != null ) {
			if ( em.getTransaction().isActive() ) {
				if ( em.getTransaction().getRollbackOnly() ) {
					em.getTransaction().rollback();
				}
				else {
					em.getTransaction().commit();
				}
			}
			em.close();
			removeEntities();
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Author.class, Hypothesis.class, Address.class };
	}
}
