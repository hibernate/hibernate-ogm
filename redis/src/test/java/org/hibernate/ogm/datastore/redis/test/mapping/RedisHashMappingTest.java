/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.mapping;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.redis.RedisProperties;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.redis.options.EntityStorageType;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.protocol.LettuceCharsets;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for mapping entities using the hash strategy.
 *
 * @author Mark Paluch
 */
public class RedisHashMappingTest extends OgmTestCase {

	@Before
	public void before() throws Exception {
		getConnection().flushall();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put(
				RedisProperties.ENTITY_STORE,
				EntityStorageType.HASH
		);
	}

	@Test
	public void canStoreAndLoadEntitiesWithIdGeneratorAndAssociation() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		Plant ficus = new Plant( 181 );
		session.persist( ficus );

		session.getTransaction().commit();

		// when
		session.getTransaction().begin();
		Plant loadedPlant = (Plant) session.get( Plant.class, ficus.getId() );

		// then
		assertThat( loadedPlant ).isNotNull();
		assertThat( loadedPlant.getHeight() ).isEqualTo( 181 );

		session.getTransaction().commit();

		session.close();
	}

	@Test
	public void canNotStoreEntitiesWithAssociation() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		Plant ficus = new Plant( 181 );
		session.persist( ficus );

		Family family = new Family( "family-1", "Moraceae", ficus );
		session.persist( family );


		try {
			// when
			session.getTransaction().commit();
			fail( "missing UnsupportedOperationException" );
		}
		catch (UnsupportedOperationException e) {
			// then
			assertThat( e ).hasMessage(
					"Cannot store value '[1]' for key 'members' to Redis, Data type java.util.ArrayList is not supported with hash storage"
			);
		}
		finally {
			session.getTransaction().rollback();
		}

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

	@Test
	public void verifyRedisRepresentation() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		Donut donut = new Donut( "homers-donut", 7.5, Donut.Glaze.Pink, "pink-donut" );
		session.persist( donut );

		session.getTransaction().commit();

		// when
		Map<String, String> map = toStringMap( getConnection().hgetall( "Donut:homers-donut".getBytes() ) );

		// then
		assertThat( map.get( "glaze" ) ).isEqualTo( "2" );
		assertThat( map.get( "radius" ) ).isEqualTo( "7.5" );
		assertThat( map.get( "alias" ) ).isEqualTo( "pink-donut" );


		session.close();
	}

	@Test
	public void verifyNullsNotStored() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		Donut donut = new Donut( "homers-donut", 7.5, Donut.Glaze.Pink, "pink-donut"  );
		session.persist( donut );

		session.getTransaction().commit();

		session.getTransaction().begin();

		Donut loaded = (Donut) session.get( Donut.class, "homers-donut" );
		loaded.setGlaze( Donut.Glaze.Dark );
		loaded.setAlias( null );
		session.persist( loaded );

		session.getTransaction().commit();

		// when
		Map<String, String> map = toStringMap( getConnection().hgetall( "Donut:homers-donut".getBytes() ) );

		// then
		assertThat( map.containsKey( "alias" ) ).isFalse();


		session.close();
	}

	private Map<String, String> toStringMap(Map<byte[], byte[]> byteMap) {
		Map<String, String> result = new HashMap<>();

		for ( Map.Entry<byte[], byte[]> entry : byteMap.entrySet() ) {
			result.put(
					new String( entry.getKey(), LettuceCharsets.UTF8 ), new String(
							entry.getValue(),
							LettuceCharsets.UTF8
					)
			);
		}

		return result;
	}

	protected RedisConnection<byte[], byte[]> getConnection() {
		return getProvider().getConnection();
	}


	private RedisDatastoreProvider getProvider() {
		return (RedisDatastoreProvider) sfi()
				.getServiceRegistry()
				.getService( DatastoreProvider.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Family.class, Plant.class, Donut.class};
	}
}
