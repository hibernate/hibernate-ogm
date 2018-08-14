/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.GridDialectType.HASHMAP;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN;
import static org.hibernate.ogm.utils.GridDialectType.INFINISPAN_REMOTE;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_EMBEDDED;
import static org.hibernate.ogm.utils.GridDialectType.NEO4J_REMOTE;

import org.hibernate.ogm.utils.SkipByGridDialect;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto
 * @author Aleksandr Mylnikov
 */
@SkipByGridDialect(
		value = { HASHMAP, INFINISPAN, INFINISPAN_REMOTE, NEO4J_EMBEDDED, NEO4J_REMOTE },
		comment = "We need to improve hql parser for dialects")
public class AggregateOperationQueryTest extends OgmJpaTestCase {

	@Before
	public void populateDb() throws Exception {
		inTransaction( em -> {
			em.persist( new Author( 1l, "Josh" ) );
			em.persist( new Author( 2l, "Ela" ) );
		} );
	}

	@After
	@Override
	public void removeEntities() throws Exception {
		super.removeEntities();
	}

	@Test
	public void shouldCountEntities() {
		inTransaction( em -> {
			Long result = (Long) em.createQuery( "SELECT COUNT(*) FROM Author author" ).getSingleResult();
			assertThat( result ).isEqualTo( 2l );
		} );
	}

	@Test
	public void shouldCountEntitiesWithCondition() {
		inTransaction( em -> {
			Long result = (Long) em.createQuery( "select count(*) from Author a WHERE id = :id" ).setParameter( "id", 1l ).getSingleResult();
			assertThat( result ).isEqualTo( 1l );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Author.class, Hypothesis.class, Address.class };
	}
}
