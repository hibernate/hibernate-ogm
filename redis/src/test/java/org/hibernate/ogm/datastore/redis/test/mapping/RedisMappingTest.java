/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.mapping;

import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.redis.test.RedisOgmTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for Redis mapping.
 *
 * @author Mark Paluch
 */
public class RedisMappingTest extends RedisOgmTestCase {

	@Before
	public void before() throws Exception {
		getConnection().flushall();
	}

	@Test
	public void canStoreAndLoadEntitiesWithIdGeneratorAndAssociation() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		Plant ficus = new Plant( 181 );
		session.persist( ficus );

		Family family = new Family( "family-1", "Moraceae", ficus );
		session.persist( family );

		session.getTransaction().commit();

		// when
		session.getTransaction().begin();
		Family loadedFamily = (Family) session.get( Family.class, "family-1" );

		// then
		assertThat( loadedFamily ).isNotNull();
		assertThat( loadedFamily.getMembers() ).onProperty( "height" ).containsExactly( 181 );

		session.getTransaction().commit();

		session.close();
	}

	@Test
	public void canStoreAndLoadEntities() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		Donut donut = new Donut( "homers-donut", 7.5, Donut.Glaze.Pink, "pink-donut" );
		session.persist( donut );


		session.getTransaction().commit();

		// when
		session.getTransaction().begin();
		Donut loadedDonut = (Donut) session.get( Donut.class, "homers-donut" );

		// then
		assertThat( loadedDonut ).isNotNull();
		assertThat( loadedDonut.getId() ).isEqualTo( "homers-donut" );
		assertThat( loadedDonut.getGlaze() ).isEqualTo( Donut.Glaze.Pink );
		assertThat( loadedDonut.getAlias() ).isEqualTo( "pink-donut" );

		session.getTransaction().commit();

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Family.class, Plant.class, Donut.class};
	}
}
