/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.options.ttl;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.redis.test.RedisOgmTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for Redis Expiry.
 *
 * @author Mark Paluch
 */
public class RedisTTLTest extends RedisOgmTestCase {

	@Before
	public void before() throws Exception {
		getConnection().flushall();
	}

	@Override
	protected void configure(Map<String, Object> cfg) {
		super.configure( cfg );
		cfg.put(
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
		Long ttl = getConnection().pttl( "LogRecord:1234" );

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
		Long bandTTL = getConnection().pttl( "Band:1" );
		Long songTTL = getConnection().pttl( "Associations:Band_Song:songs:1" );

		// then
		assertThat( bandTTL ).isEqualTo( -1L );
		assertThat( songTTL ).isGreaterThan( TimeUnit.SECONDS.toMillis( 50 ) );

		session.close();
	}


	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {LogRecord.class, Band.class, Song.class};
	}
}
