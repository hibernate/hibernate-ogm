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
 * If the @CacheTemplate annotation indicates a not existing cache a validation error is issued.
 *
 * The test defines the expected behaviour:
 *
 * GIVEN no template defined THEN the cache will be created without a template.
 * GIVEN a template defined by annotation THEN the cache will be created with template defined by annotation
 * GIVEN a template defined by property THEN the cache will be created with template define by property
 * GIVEN a template defined on property and another on defined by annotation THEN the cache will be created with template defined by annotation,
 * 	template defined by property will be ignored
 * GIVEN a template, not present on server configuration, defined by annotation THEN an {@link HibernateException} will be thrown
 * GIVEN a template, not present on server configuration, defined by property THEN an {@link HibernateException} will be thrown
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
				Collections.singletonMap( InfinispanRemoteProperties.USE_CACHE_AS_TEMPLATE, "default" ),
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
				Collections.singletonMap( InfinispanRemoteProperties.USE_CACHE_AS_TEMPLATE, "notExist" ),
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

			RemoteCache cache = remoteCacheManager.getCache( cacheName );
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
				Collections.singletonMap( InfinispanRemoteProperties.USE_CACHE_AS_TEMPLATE, "notExist" ),
				NoAnnotationEntity.class
		);

		fail( "Expected exception at Hibernate factory creation time was not raised" );

	}

}
