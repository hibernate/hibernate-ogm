/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.options.ttl;

import java.util.concurrent.TimeUnit;

import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.RedisConnection;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for Redis Expiry.
 *
 * @author Mark Paluch
 */
public class GlobalPropertyTTLTest extends OgmTestCase {

	@Before
	public void before() throws Exception {
		getConnection().flushall();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getProperties().put(
				DocumentStoreProperties.ASSOCIATIONS_STORE,
				AssociationStorageType.ASSOCIATION_DOCUMENT
		);
	}

	@Test
	public void ttlOnEntities() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		LogRecord record = new LogRecord( "1234" );
		session.persist( record );


		session.getTransaction().commit();

		// when
		Long ttl = getConnection().pttl( "LogRecord:1234".getBytes() );

		// then
		assertThat( ttl ).isGreaterThanOrEqualTo( TimeUnit.DAYS.toMillis( 6 ) );

		session.close();
	}

	@Test
	public void ttlOnMappings() {
		OgmSession session = openSession();
		session.getTransaction().begin();

		// given

		Song healTheWorld = new Song( "heal the world" );
		session.persist( healTheWorld );

		Band band = new Band( "1", "one-hit-wonder", healTheWorld );
		session.persist( band );

		session.getTransaction().commit();

		// when
		Long bandTTL = getConnection().pttl( "Band:1".getBytes() );
		Long songTTL = getConnection().pttl( "Associations:Band:1".getBytes() );

		// then
		assertThat( bandTTL ).isEqualTo( -1L );
		assertThat( songTTL ).isGreaterThan( TimeUnit.SECONDS.toMillis( 50 ) );

		session.close();
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
		return new Class<?>[] {LogRecord.class, Band.class, Song.class};
	}
}
