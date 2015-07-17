/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.id;

import org.hibernate.Transaction;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.redis.RedisDialect;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.utils.OgmTestCase;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Tests for id generators in Redis
 *
 * @author Mark Paluch
 */
public class TableGeneratorTest extends OgmTestCase {

	@Test
	public void canUseSpecificValueColumNames() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		PianoPlayer ken = new PianoPlayer( "Ken Gold" );
		GuitarPlayer buck = new GuitarPlayer( "Buck Cherry" );
		// when
		session.persist( ken );
		session.persist( buck );

		tx.commit();
		session.clear();
		tx = session.beginTransaction();
		ken = (PianoPlayer) session.load( PianoPlayer.class, ken.getId() );
		buck = (GuitarPlayer) session.load( GuitarPlayer.class, buck.getId() );

		// then
		assertThat( ken.getId() ).isEqualTo( 1L );
		assertThat( buck.getId() ).isEqualTo( 1L );
		assertCountQueryResult( "Identifiers", "pianoPlayer", "1" );
		assertCountQueryResult( "Identifiers", "guitarPlayer", "1" );

		tx.commit();
		session.close();
	}

	private void assertCountQueryResult(String hash, String field, String expectedCount) {

		String actualCount = RedisDialect.toString(
				getProvider().getConnection().hget(
						RedisDialect.toBytes( hash ),
						RedisDialect.toBytes( field )
				)
		);

		assertThat( actualCount ).describedAs( "Count query didn't yield expected result" ).isEqualTo( expectedCount );
	}

	private RedisDatastoreProvider getProvider() {
		return (RedisDatastoreProvider) sfi()
				.getServiceRegistry()
				.getService( DatastoreProvider.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {PianoPlayer.class, GuitarPlayer.class};
	}
}
