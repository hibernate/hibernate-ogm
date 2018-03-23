/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.configuration;

import static org.fest.assertions.Fail.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.options.cache.CacheConfiguration;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteTestHelper;
import org.hibernate.ogm.datastore.infinispanremote.utils.RemoteHotRodServerRule;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.TestHelper;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

/**
 * Test for the {@link CacheConfiguration} annotation.
 * <p>
 * Check that when the annotation is present the selected configuration is used when the
 * associated entity cache is created.
 * <p>
 * For the selected entity, the annotation overrides the default global configuration set
 * using {@link InfinispanRemoteProperties#CACHE_CONFIGURATION}
 *
 * @author Fabio Massimo Ercoli
 * @see <a href="https://hibernate.atlassian.net/browse/OGM-1329">OGM-1329</a>
 * @see <a href="https://hibernate.atlassian.net/browse/OGM-1405">OGM-1405</a>
 */
@TestForIssue(jiraKey = { "OGM-1329", "OGM-1405" })
public class CacheConfigurationValidationTest {

	@ClassRule
	public static final RemoteHotRodServerRule hotRodServer = new RemoteHotRodServerRule();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testNoConfigurationDefined() {
		String cacheName = NoAnnotationEntity.class.getSimpleName();
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory( NoAnnotationEntity.class ) ) {
			InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
			String configuration = provider.getConfiguration( cacheName );
			assertNull( "No configuration applied to the cache", configuration );
		}
		verifyCacheExistThenRemoveIt( cacheName );
	}

	@Test
	public void testConfigurationDefinedByAnnotation() {
		String cacheName = ExistCacheConfigurationEntity.class.getSimpleName();
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory( ExistCacheConfigurationEntity.class ) ) {

			InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
			String configuration = provider.getConfiguration( cacheName );
			assertEquals( "Configuration 'ogm-config' is applied to the cache", "ogm-config", configuration );
		}
		verifyCacheExistThenRemoveIt( cacheName );
	}

	@Test
	public void testConfigurationDefinedByProperty() {
		String cacheName = NoAnnotationEntity.class.getSimpleName();
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory(
				Collections.singletonMap( InfinispanRemoteProperties.CACHE_CONFIGURATION, "ogm-config" ),
				NoAnnotationEntity.class
		) ) {

			InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
			String configuration = provider.getConfiguration( cacheName );
			assertEquals( "Configuration 'ogm-config' is applied to the cache", "ogm-config", configuration );
		}
		verifyCacheExistThenRemoveIt( cacheName );
	}

	@Test
	public void testConfigurationDefinedByPropertyOverridesConfigurationDefinedByAnnotation() {
		String cacheName = ExistCacheConfigurationEntity.class.getSimpleName();
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory(
				Collections.singletonMap( InfinispanRemoteProperties.CACHE_CONFIGURATION, "notExist" ),
				ExistCacheConfigurationEntity.class
		) ) {
			InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
			String configuration = provider.getConfiguration( cacheName );
			assertEquals( "Configuration 'ogm-config' is applied to the cache", "ogm-config", configuration );
		}
		verifyCacheExistThenRemoveIt( cacheName );
	}

	private void verifyCacheExistThenRemoveIt(String cacheName) {
		RemoteCacheManager remoteCacheManager = new RemoteCacheManager();
		try {
			RemoteCache<?, ?> cache = remoteCacheManager.getCache( cacheName );
			assertNotNull( "Dialect should create cache", cache );
			remoteCacheManager.administration().removeCache( cacheName );
		}
		finally {
			remoteCacheManager.stop();
		}
	}

	@Test
	public void testConfigurationDefinedByAnnotationButDoesNotExist() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage(
				"OGM001718: The remote cache configurations '[notExist]' were expected to exist but are not defined on the server" );

		TestHelper.getDefaultTestSessionFactory( NotExistCacheConfigurationEntity.class );
		fail( "Expected exception at Hibernate factory creation time was not raised" );
	}

	@Test
	public void testConfigurationDefinedByPropertyButDoesNotExist() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage(
				"OGM001718: The remote cache configurations '[notExist]' were expected to exist but are not defined on the server" );

		TestHelper.getDefaultTestSessionFactory(
				Collections.singletonMap( InfinispanRemoteProperties.CACHE_CONFIGURATION, "notExist" ),
				NoAnnotationEntity.class
		);
		fail( "Expected exception at Hibernate factory creation time was not raised" );
	}

}
