/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.initialize;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.utils.RemoteHotRodServerRule;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Hibernate OGM should throw an exception when the cache does not exist and we don't want to create it.
 * <p>
 * It's weird but we have two different exceptions based on the fact of having one cache missing or two.
 *
 * @author Davide D'Alto
 */
@TestForIssue(jiraKey = "OGM-1170")
public class CacheNotDefinedTest {

	@ClassRule
	public static final RemoteHotRodServerRule hotRodServer = new RemoteHotRodServerRule();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void undefinedCacheOnServer() throws Exception {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001709: " );

		createFactory( EntityCache.class );
	}

	@Test
	public void undefinedCachesOnServer() throws Exception {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001710: " );

		createFactory( AnotherEntityCache.class, EntityCache.class );
	}

	private void createFactory(Class<?>... entities) {
		Map<String, Object> settings = new HashMap<>();
		settings.put( OgmProperties.DATASTORE_PROVIDER, "infinispan_remote" );
		settings.put( InfinispanRemoteProperties.CONFIGURATION_RESOURCE_NAME, "hotrod-client-testingconfiguration.properties" );
		// This is the important option
		settings.put( OgmProperties.CREATE_DATABASE, false );

		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory( settings, entities ) ) {
			Assert.fail( "This should have refused to boot because the caches don't exist" );
		}
	}

	@Entity
	@Table(name = "ENTITY_CACHE")
	private static class EntityCache {

		@Id
		String id;

		@SuppressWarnings("unused")
		String description;
	}

	@Entity
	@Table(name = "ANOTHER_ENTITY_CACHE")
	private static class AnotherEntityCache {

		@Id
		String id;

		@SuppressWarnings("unused")
		String description;
	}
}
