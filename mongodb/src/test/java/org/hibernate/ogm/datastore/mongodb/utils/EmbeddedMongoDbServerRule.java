/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.mongodb.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.ogm.datastore.mongodb.logging.impl.Log;
import org.hibernate.ogm.datastore.mongodb.logging.impl.LoggerFactory;

import org.junit.rules.ExternalResource;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

/**
 * Start embedded MongoDB server if $MONGODB_HOSTNAME:$MONGODB_PORT is available.
 * Eventually, close it after test ran.
 *
 * @author The Viet Nguyen
 */
public class EmbeddedMongoDbServerRule extends ExternalResource {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private static final AtomicBoolean running = new AtomicBoolean();

	private static final Map<String, String> env = System.getenv();
	private static final String MONGODB_HOSTNAME = env.getOrDefault( "MONGODB_HOSTNAME", "localhost" );
	private static final int MONGODB_PORT = Integer.parseInt( env.getOrDefault( "MONGODB_PORT", "27018" ) );

	private MongodExecutable mongodExecutable;

	@Override
	protected void before() throws Exception {
		synchronized (running) {
			if ( running.compareAndSet( false, true ) && portOpen() ) {
				log.info( "Starting embedded MongoDb server..." );
				MongodStarter starter = MongodStarter.getDefaultInstance();
				IMongodConfig mongodConfig = new MongodConfigBuilder()
						.version( Version.Main.PRODUCTION )
						.net( new Net( MONGODB_HOSTNAME, MONGODB_PORT, Network.localhostIsIPv6() ) )
						.build();
				mongodExecutable = starter.prepare( mongodConfig );
				startMongoDb();
			}
		}
	}

	@Override
	protected void after() {
		synchronized (running) {
			if ( mongodExecutable != null ) {
				running.set( false );
				mongodExecutable.stop();
			}
		}
	}

	private boolean portOpen() {
		try {
			Socket sock = new Socket( MONGODB_HOSTNAME, MONGODB_PORT );
			sock.close();
		}
		catch (IOException e) {
			if ( e.getMessage().contains( "refused" ) ) {
				return true;
			}
		}
		return false;
	}

	private void startMongoDb() {
		try {
			mongodExecutable.start();
			log.infof( "Embedded MongoDb server started at %s:%s", MONGODB_HOSTNAME, MONGODB_PORT );
		}
		catch (IOException e) {
			log.debug( "Cannot start embedded MongoDb. Please ignore this message if you have already started an instance of MongoDb elsewhere", e );
		}
	}
}
