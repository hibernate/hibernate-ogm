/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.utils;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.infinispan.client.hotrod.test.HotRodClientTestingUtil;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.test.TestingUtil;

/**
 * A JUnit rule which starts the Hot Rod server, and finally closes it.
 *
 * @author Sanne Grinovero
 */
public final class RemoteHotRodServerRule extends org.junit.rules.ExternalResource {

	public static final String CACHEMANAGER_CONFIGURATION_RESOURCE = "hotrod-server-singleton.xml";
	public static final int HOTRODSERVER_PORT = 11222;

	/**
	 * An atomic static flag to make it possible to reuse this class
	 * both as a global JUnit listener and as a Rule, and avoid
	 * starting the server twice.
	 */
	private static final AtomicBoolean running = new AtomicBoolean();

	private EmbeddedCacheManager manager;
	private HotRodServer hotRodServer;

	@Override
	public void before() throws Exception {
		//Synchronize on the static field to defend against concurrent launches,
		//e.g. the usage as JUnit Rule concurrently with the usage as global test listener in Surefire.
		synchronized ( running ) {
			if ( running.compareAndSet( false, true ) ) {
				ConfigurationBuilderHolder cfgBuilder;
				try ( InputStream inputStream = FileLookupFactory.newInstance()
						.lookupFileStrict( CACHEMANAGER_CONFIGURATION_RESOURCE, RemoteHotRodServerRule.class.getClassLoader() ) ) {
					cfgBuilder = new ParserRegistry().parse( inputStream );
				}
				manager = new DefaultCacheManager( cfgBuilder, true );
				HotRodServerConfigurationBuilder hotRodServerConfigurationBuilder = new HotRodServerConfigurationBuilder()
						.port( HOTRODSERVER_PORT )
						.host( "localhost" );
				hotRodServer = HotRodClientTestingUtil.startHotRodServer( manager, HOTRODSERVER_PORT, hotRodServerConfigurationBuilder );
			}
		}
	}

	@Override
	public void after() {
		synchronized ( running ) {
			if ( hotRodServer != null ) {
				running.set( false );
				hotRodServer.stop();
			}
			TestingUtil.killCacheManagers( manager );
		}
	}

	/**
	 * Clears the state of all caches in the Infinispan server.
	 * Warning: this wipes out both the data and the Protobuf schema.
	 */
	public void resetHotRodServerState() throws Exception {
		synchronized ( running ) {
			final EmbeddedCacheManager cm = manager;
			if ( cm != null ) {
				cm.getCacheNames().forEach(
						name -> cm.getCache( name ).clear()
					);
			}
		}
	}

}
