/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.mapping;

import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.redis.test.RedisOgmTestCase;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;

import org.junit.Before;
import org.junit.Test;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Test for Redis JSON mapping.
 *
 * @author Mark Paluch
 */
@SkipByGridDialect(GridDialectType.REDIS_HASH)
public class RedisJsonMappingTest extends RedisOgmTestCase {

	@Before
	public void before() throws Exception {
		getConnection().flushall();
	}

	@Test
	public void verifyRedisRepresentation() throws JSONException {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		Donut donut = new Donut( "homers-donut", 7.5, Donut.Glaze.Pink, "pink-donut" );
		session.persist( donut );

		session.getTransaction().commit();

		// when
		String representation = getConnection().get( "Donut:homers-donut" );

		// then
		JSONAssert.assertEquals(
				"{'alias':'pink-donut','radius':7.5,'glaze':2}",
				representation,
				JSONCompareMode.STRICT
		);

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
		String familyRepresentation = getConnection().get( "Family:family-1" );
		String plantRepresentation = getConnection().get( "Plant:1" );

		// then
		JSONAssert.assertEquals(
				"{\"members\":[1],\"name\":\"Moraceae\"}",
				familyRepresentation,
				JSONCompareMode.STRICT
		);

		JSONAssert.assertEquals(
				"{\"height\":181}",
				plantRepresentation,
				JSONCompareMode.STRICT
		);

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Family.class, Plant.class, Donut.class};
	}
}
