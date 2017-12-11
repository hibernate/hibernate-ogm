/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.cache;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.impl.InfinispanRemoteDatastoreProvider;
import org.hibernate.ogm.datastore.infinispanremote.schema.spi.MapSchemaCapture;
import org.hibernate.ogm.datastore.infinispanremote.utils.RemoteHotRodServerRule;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.utils.TestHelper;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NoTemplateTest {

	@ClassRule
	public static final RemoteHotRodServerRule hotRodServer = new RemoteHotRodServerRule();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void noTemplateTest() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM001709:" );
		getDatastoreProvider( CacheEntity.class );
	}

	public static void getDatastoreProvider(Class<?> entity) {
		Map<String, Object> settings = settings();
		MapSchemaCapture schemaCapture = new MapSchemaCapture();
		settings.put( InfinispanRemoteProperties.SCHEMA_CAPTURE_SERVICE, schemaCapture );
		try ( SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory( settings, entity ) ) {
			InfinispanRemoteDatastoreProvider provider = (InfinispanRemoteDatastoreProvider) ( (SessionFactoryImplementor) sessionFactory )
					.getServiceRegistry().getService( DatastoreProvider.class );
			Assert.assertNotNull( provider.getCache( entity.getSimpleName() ) );
		}
	}

	public static Map<String, Object> settings() {
		Map<String, Object> settings = new HashMap<>();
		settings.put( OgmProperties.DATASTORE_PROVIDER, "infinispan_remote" );
		settings.put(
				InfinispanRemoteProperties.CONFIGURATION_RESOURCE_NAME,
				"hotrod-client-testingconfiguration.properties"
		);
		settings.put( OgmProperties.CREATE_DATABASE, true );
		return settings;
	}

}
