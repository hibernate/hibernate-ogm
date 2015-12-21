/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.mapping;

import java.util.List;
import java.util.Map;

import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.redis.test.RedisOgmTestCase;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;

import org.junit.Before;
import org.junit.Test;

import org.json.JSONException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

/**
 * Test for Redis Hash mapping.
 *
 * @author Mark Paluch
 */
@SkipByGridDialect(GridDialectType.REDIS)
public class RedisHashMappingTest extends RedisOgmTestCase {

	@Before
	public void before() throws Exception {
		getConnection().flushall();
	}

	@Test
	public void verifyRedisRepresentation() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		Donut donut = new Donut( "homers-donut", 7.5, Donut.Glaze.Pink, "pink-donut" );
		session.persist( donut );

		session.getTransaction().commit();

		// when
		Map<String, String> map = getConnection().hgetall( "Donut:homers-donut" );

		// then
		assertThat( map ).includes( entry( "alias", "pink-donut" ) );
		assertThat( map ).includes( entry( "radius", "7.5" ) );
		assertThat( map ).includes( entry( "glaze", "2" ) );

		session.close();
	}

	@Test
	public void canStoreAndLoadEntitiesWithIdGeneratorAndAssociation() throws JSONException {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		Plant ficus = new Plant( 181 );
		session.persist( ficus );

		Family family = new Family( "family-1", "Moraceae", ficus );
		session.persist( family );

		session.getTransaction().commit();

		// when
		Map<String, String> familyRepresentation = getConnection().hgetall( "Family:family-1" );
		Map<String, String> plantRepresentation = getConnection().hgetall( "Plant:1" );
		List<String> associationRepresentation = getConnection().lrange( "Associations:Family_Plant:family-1", 0, -1 );

		// then

		assertThat( familyRepresentation ).includes( entry( "name", "Moraceae" ) ).includes(
				entry(
						"id",
						"family-1"
				)
		);
		assertThat( plantRepresentation ).includes( entry( "height", "181" ) ).includes( entry( "id", "1" ) );
		assertThat( associationRepresentation ).contains( "\"1\"" );

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Family.class, Plant.class, Donut.class};
	}
}
