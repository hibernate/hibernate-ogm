/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.redis;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.datastore.redis.test.RedisOgmTestCase;
import org.hibernate.ogm.datastore.redis.test.options.ttl.Band;
import org.hibernate.ogm.datastore.redis.test.options.ttl.Song;
import org.hibernate.ogm.datastore.redis.utils.RedisTestHelper;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.TuplesSupplier;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;

import org.junit.Test;

import com.google.common.io.Resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

/**
 * Test for Redis Cluster {@link AbstractRedisDialect#scan(com.lambdaworks.redis.KeyScanCursor, com.lambdaworks.redis.ScanArgs)} that
 * is used in {@link org.hibernate.ogm.dialect.spi.GridDialect#forEachTuple(ModelConsumer, org.hibernate.ogm.model.key.spi.EntityKeyMetadata...)}
 *
 * @author Mark Paluch
 */
public class RedisDialectClusterForEachTest extends RedisOgmTestCase {

	@Test
	public void testScan() throws Exception {

		AbstractRedisDialect dialect = RedisTestHelper.getDialect( getProvider() );
		assumeTrue( dialect.isClusterMode() );

		// pre-computed key file.
		URL resource = Resources.getResource( "redis-cluster-slothashes.txt" );
		List<String> lines = Resources.readLines( resource, StandardCharsets.ISO_8859_1 );

		OgmSession session = openSession();
		session.getTransaction().begin();

		// given
		int availableKeys = 0;
		for ( String line : lines ) {

			if ( line.startsWith( "#" ) || line.trim().isEmpty() ) {
				continue;
			}

			String key = line.substring( 0, line.indexOf( ' ' ) ).trim();

			Band record = new Band( key, key );
			session.persist( record );
			availableKeys++;
		}
		session.getTransaction().commit();

		final AtomicInteger counter = new AtomicInteger();

		dialect.forEachTuple( new ModelConsumer() {
			@Override
			public void consume(TuplesSupplier supplier) {
				try ( ClosableIterator<Tuple> closableIterator = supplier.get( null ) ) {
					while ( closableIterator.hasNext() ) {
						counter.incrementAndGet();
					}
				}
			}
		}, null, new DefaultEntityKeyMetadata( "Band", new String[] {"id"} ) );

		assertEquals( availableKeys, counter.get() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Band.class, Song.class
		};
	}
}
