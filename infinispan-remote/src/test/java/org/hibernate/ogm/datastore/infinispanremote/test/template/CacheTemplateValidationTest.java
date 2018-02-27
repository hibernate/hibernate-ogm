/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.template;

import static org.fest.assertions.Fail.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.options.cache.CacheTemplate;
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
 * Test for the {@link CacheTemplate} annotation.
 * <p>
 * Check that when the annotation is present the selected template is used when the
 * associated entity cache is created.
 * <p>
 * For the selected entity, the annotation overrides the default global template set
 * using {@link InfinispanRemoteProperties#NEW_CACHE_TEMPLATE}
 *
 * @author Fabio Massimo Ercoli
 * @see <a href="https://hibernate.atlassian.net/browse/OGM-1329">OGM-1329</a>
 * @see <a href="https://hibernate.atlassian.net/browse/OGM-1405">OGM-1405</a>
 */
@TestForIssue(jiraKey = { "OGM-1329", "OGM-1405" })
public class CacheTemplateValidationTest {

	@ClassRule
	public static final RemoteHotRodServerRule hotRodServer = new RemoteHotRodServerRule();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void test_noTemplateDefined() {
		String cacheName = NoAnnotationEntity.class.getSimpleName();
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory( NoAnnotationEntity.class ) ) {
			InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
			String template = provider.getTemplate( cacheName );
			assertNull( "No template applied to the cache", template );
		}
		verifyCacheExistThenRemoveIt( cacheName );
	}

	@Test
	public void test_templateDefinedByAnnotation() {
		String cacheName = ExistCacheTemplateEntity.class.getSimpleName();
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory( ExistCacheTemplateEntity.class ) ) {

			InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
			String template = provider.getTemplate( cacheName );
			assertEquals( "Template default applied to the cache", "default", template );
		}
		verifyCacheExistThenRemoveIt( cacheName );
	}

	@Test
	public void test_templateDefinedByProperty() {
		String cacheName = NoAnnotationEntity.class.getSimpleName();
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory(
				Collections.singletonMap( InfinispanRemoteProperties.NEW_CACHE_TEMPLATE, "default" ),
				NoAnnotationEntity.class
		) ) {

			InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
			String template = provider.getTemplate( cacheName );
			assertEquals( "Template default applied to the cache", "default", template );
		}
		verifyCacheExistThenRemoveIt( cacheName );
	}

	@Test
	public void test_templateDefinedByProperty_overrides_templateDefinedByAnnotation() {
		String cacheName = ExistCacheTemplateEntity.class.getSimpleName();
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory(
				Collections.singletonMap( InfinispanRemoteProperties.NEW_CACHE_TEMPLATE, "notExist" ),
				ExistCacheTemplateEntity.class
		) ) {
			InfinispanRemoteDatastoreProvider provider = InfinispanRemoteTestHelper.getProvider( sessionFactory );
			String template = provider.getTemplate( cacheName );
			assertEquals( "Template default applied to the cache", "default", template );
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
	public void test_templateDefinedByAnnotation_butDoesNotExist() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage(
				"OGM001709: The remote cache 'notExist' was expected to exist but is not defined on the server" );

		TestHelper.getDefaultTestSessionFactory( NotExistCacheTemplateEntity.class );
		fail( "Expected exception at Hibernate factory creation time was not raised" );
	}

	@Test
	public void test_templateDefinedByProperty_butDoesNotExist() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage(
				"OGM001709: The remote cache 'notExist' was expected to exist but is not defined on the server" );

		TestHelper.getDefaultTestSessionFactory(
				Collections.singletonMap( InfinispanRemoteProperties.NEW_CACHE_TEMPLATE, "notExist" ),
				NoAnnotationEntity.class
		);
		fail( "Expected exception at Hibernate factory creation time was not raised" );
	}

}
