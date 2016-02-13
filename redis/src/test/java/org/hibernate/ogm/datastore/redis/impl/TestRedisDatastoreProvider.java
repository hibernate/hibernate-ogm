/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import java.util.concurrent.TimeUnit;

import org.hibernate.ogm.cfg.spi.Hosts;

import com.lambdaworks.redis.RedisClient;

/**
 * Data store provider that reused the {@link com.lambdaworks.redis.RedisClient} throughout the test run.
 *
 * @author Mark Paluch
 */
public class TestRedisDatastoreProvider extends RedisDatastoreProvider {

	private static volatile RedisClient staticInstance;

	@Override
	protected RedisClient createClient(Hosts.HostAndPort hostAndPort) {
		if ( staticInstance == null ) {
			final RedisClient redisClient = super.createClient( hostAndPort );

			Runtime.getRuntime().addShutdownHook(
					new Thread( "RedisClient shutdown hook" ) {
						@Override
						public void run() {
							redisClient.shutdown( 0, 0, TimeUnit.MILLISECONDS );
						}
					}
			);
			staticInstance = redisClient;
		}

		return staticInstance;

	}

	@Override
	protected void shutdownClient() {
		// noop. Client is closed using a ShutdownHook
	}
}
