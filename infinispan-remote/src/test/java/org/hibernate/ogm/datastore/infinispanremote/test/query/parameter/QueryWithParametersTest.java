/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.query.parameter;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.hibernate.ogm.backendtck.queries.parameters.Movie;
import org.hibernate.ogm.datastore.infinispanremote.test.mapping.ByteEntity;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteJpaServerRunner;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.OgmJpaTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Alternative version of {@link org.hibernate.ogm.backendtck.queries.parameters.QueryWithParametersTest}
 * to test querying on {@link Byte} field,
 * because the original uses a {@link Byte} field with a starting with 'v' name: {@link Movie#viewerRating}.
 *
 * @see QueryWithParametersTest#canUseByteForSimpleComparison
 * @see QueryWithParametersTest#canUseByteAsParameterForSimpleComparison
 * @see QueryWithParametersTest#canUseByteAsParameterForInComparison
 * @author Fabio Massimo Ercoli
 */
@RunWith(InfinispanRemoteJpaServerRunner.class)
@TestForIssue(jiraKey = "OGM-1458")
public class QueryWithParametersTest extends OgmJpaTestCase {

	private ByteEntity entityA;
	private ByteEntity entityB;
	private ByteEntity entityC;

	@Before
	public void setup() {
		entityA = new ByteEntity( 1, (byte) 7 );
		entityB = new ByteEntity( 2, (byte) 3 );
		entityC = new ByteEntity( 3, (byte) 9 );

		inTransaction( em -> {
			em.persist( entityA );
			em.persist( entityB );
			em.persist( entityC );
		} );
	}

	@After
	public void teardown() {
		inTransaction( em -> {
			em.refresh( entityA );
			em.refresh( entityB );
			em.refresh( entityC );

			em.remove( entityA );
			em.remove( entityB );
			em.remove( entityC );
		} );
	}

	@Test
	public void canUseByteForSimpleComparison() {
		inTransaction( em -> {
			List<ByteEntity> entities = em.createQuery( "SELECT e FROM ByteEntity e WHERE e.counter = 3", ByteEntity.class )
					.getResultList();

			assertThat( entities ).containsExactly( entityB );
		} );
	}

	@Test
	public void canUseByteAsParameterForSimpleComparison() {
		inTransaction( em -> {
			List<ByteEntity> entities = em.createQuery( "SELECT e FROM ByteEntity e WHERE e.counter = :counter", ByteEntity.class )
					.setParameter( "counter", (byte) 3 )
					.getResultList();

			assertThat( entities ).containsExactly( entityB );
		} );
	}

	@Test
	public void canUseByteAsParameterForInComparison() {
		inTransaction( em -> {
			List<ByteEntity> entities = em.createQuery( "SELECT e FROM ByteEntity e WHERE e.counter IN (:counters)", ByteEntity.class )
					.setParameter( "counters", Arrays.asList( (byte) 7, (byte) 9 ) )
					.getResultList();

			assertThat( entities ).containsOnly( entityA, entityC );
		} );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ByteEntity.class };
	}
}
