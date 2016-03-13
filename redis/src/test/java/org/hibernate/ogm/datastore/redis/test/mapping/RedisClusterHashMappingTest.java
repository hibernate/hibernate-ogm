/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.mapping;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.redis.AbstractRedisDialect;
import org.hibernate.ogm.datastore.redis.test.RedisOgmTestCase;
import org.hibernate.ogm.datastore.redis.utils.RedisTestHelper;
import org.hibernate.ogm.utils.GridDialectType;
import org.hibernate.ogm.utils.SkipByGridDialect;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.cluster.SlotHash;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import org.json.JSONException;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.junit.Assume.assumeTrue;

/**
 * Test for Redis Hash mapping using Redis Cluster.
 *
 * @author Mark Paluch
 */
@SkipByGridDialect(GridDialectType.REDIS_JSON)
public class RedisClusterHashMappingTest extends RedisOgmTestCase {

	@Before
	public void before() throws Exception {
		AbstractRedisDialect dialect = RedisTestHelper.getDialect( getProvider() );
		assumeTrue( dialect.isClusterMode() );
		getConnection().flushall();
	}

	@Test
	public void verifyHashSlotCalculation() throws JSONException, ExecutionException, InterruptedException {
		OgmSession session = openSession();

		RedisClusterCommands<String, String> connection = getConnection();

		// given
		int expectedSlot = SlotHash.getSlot( "zoomzoom" );

		// when
		long calculatedSlot = connection.clusterKeyslot( "{zoomzoom}.ClusterDonut:blueberry" );

		// then
		assertThat( calculatedSlot ).isEqualTo( expectedSlot );

		session.close();
	}

	@Test
	public void canStoreAndLoadEntities() throws JSONException {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		ClusterDonut clusterDonut = new ClusterDonut( "blueberry", 3.1415, ClusterDonut.Glaze.Dark, "mhhh" );
		session.persist( clusterDonut );

		session.getTransaction().commit();

		// when
		Map<String, String> donutRepresentation = getConnection().hgetall( "{zoomzoom}.ClusterDonut:blueberry" );

		// then

		assertThat( donutRepresentation ).includes( entry( "id", "blueberry" ) ).includes(
				entry(
						"alias",
						"mhhh"
				)
		);

		session.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {ClusterDonut.class};
	}
}
