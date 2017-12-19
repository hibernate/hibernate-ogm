/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.utils;

import java.io.InputStream;

import org.hibernate.AssertionFailure;
import org.infinispan.Cache;
import org.infinispan.commons.util.FileLookupFactory;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.TestingUtil;
import org.junit.Assert;
import org.junit.rules.ExternalResource;

/**
 * Helper to manage a clustered Infinispan CacheManager:
 * designed to be rather defensive and avoid some common pitfalls.
 * <p>
 * Example:
 * <p>
 * <pre>
 * {@code
 *     @Rule
 *     public final InfinispanNode nodeA = new InfinispanNode( "nodeA", "infinispan-dist.xml" );
 *     @Rule
 *     public final InfinispanNode nodeB = new InfinispanNode( "nodeB", "infinispan-dist.xml" );
 *
 *     @Test
 *     public void testExample() throws Exception {
 *         EmbeddedCacheManager manager = nodeA.getCacheManager( 2 ); // Number of nodes this cluster should have
 *         ...
 *     }
 * }
 * </pre>
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2014 Red Hat Inc.
 */
public class InfinispanNode extends ExternalResource {

	private static final long DEFAULT_JOIN_TIMEOUT_MS = 15000;

	private static final String JGROUPS_HOSTNAME = System.getProperty( "jgroups.bind_addr" );
	private static final Boolean IPV4_STACK = Boolean.getBoolean( "java.net.preferIPv4Stack" );

	private final String configurationFile;
	private final long joinTimeoutMilliseconds;
	private final String cacheManagerName;
	private EmbeddedCacheManager manager;
	private volatile boolean started = false;
	private volatile boolean groupFormed = false;

	public InfinispanNode(final String cacheManagerName, final String configurationFile) {
		this( cacheManagerName, configurationFile, DEFAULT_JOIN_TIMEOUT_MS );
	}

	public InfinispanNode(final String cacheManagerName, final String configurationFile, final long joinTimeoutMilliseconds) {
		this.cacheManagerName = cacheManagerName;
		this.configurationFile = configurationFile;
		this.joinTimeoutMilliseconds = joinTimeoutMilliseconds;
	}

	public EmbeddedCacheManager getCacheManager(final int expectedGroupSize) {
		if ( ! started ) {
			throw new IllegalStateException( "The before() method of this Rule was not triggered (or did not complete succesfully)" );
		}
		if ( manager == null ) {
			throw new AssertionFailure( "Unexpected state: the CacheManager not created?" );
		}
		if ( ! groupFormed && expectedGroupSize != 1 ) {
			verifyNetworkStackEnabled();
			verifyConfiguredAsClustered( manager );
			TestingUtil.blockUntilViewReceived( manager.getCache(), expectedGroupSize, joinTimeoutMilliseconds, true );
		}
		return manager;
	}

	public static void verifyNetworkStackEnabled() {
		Assert.assertEquals( "Environment property '-Djgroups.bind_addr=127.0.0.1' was not detected", "127.0.0.1", JGROUPS_HOSTNAME );
		Assert.assertTrue( "Environment property '-Djava.net.preferIPv4Stack=true' was not detected", IPV4_STACK.booleanValue() );
	}

	@Override
	public void before() throws Throwable {
		InputStream inputStream = FileLookupFactory.newInstance().lookupFileStrict( configurationFile, InfinispanNode.class.getClassLoader() );
		ConfigurationBuilderHolder cfgBuilder = new ParserRegistry().parse( inputStream );
		cfgBuilder.getGlobalConfigurationBuilder().globalJmxStatistics().cacheManagerName( cacheManagerName );
		manager = new DefaultCacheManager( cfgBuilder, true );
		System.out.println( "Started CacheManager" );
		//Now *Actually* start it:
		manager.getCache();
		started = true;
	}

	@Override
	public void after() {
		TestingUtil.killCacheManagers( manager );
	}

	public static void verifyConfiguredAsClustered(final EmbeddedCacheManager cacheManager) {
		final GlobalConfiguration globalConfiguration = cacheManager.getCacheManagerConfiguration();
		Assert.assertTrue( "This CacheManager is not configured for clustering", globalConfiguration.isClustered() );
		Assert.assertNotNull( "This CacheManager is configured for clustering but the Transport was not found", cacheManager.getTransport() );
	}

	public static void verifyConfiguredAsClustered(final Cache<?, ?> cache) {
		verifyConfiguredAsClustered( cache.getCacheManager() );
		final Configuration cacheConfiguration = cache.getCacheConfiguration();
		Assert.assertTrue( "This Cache is managed by a clustered CacheManager, but the Cache is having clustering disabled!", cacheConfiguration.clustering().cacheMode().isClustered() );
	}

	/**
	 * For manual tests and debugging
	 */
	public static void main(String[] args) throws Throwable {
		final InfinispanNode nodeA = new InfinispanNode( "nodeA", "infinispan-dist.xml" );
		nodeA.before();
		try {
			final InfinispanNode nodeB = new InfinispanNode( "nodeB", "infinispan-dist.xml" );
			nodeB.before();
			try {
				nodeA.getCacheManager( 2 );
				nodeB.getCacheManager( 2 );
			}
			finally {
				nodeB.after();
			}
		}
		finally {
			nodeA.after();
		}
	}

}
