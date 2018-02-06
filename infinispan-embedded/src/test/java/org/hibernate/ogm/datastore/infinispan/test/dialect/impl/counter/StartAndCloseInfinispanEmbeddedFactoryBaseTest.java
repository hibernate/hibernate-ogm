/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl.counter;

import static org.fest.assertions.Fail.fail;

import java.util.Map;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.infinispan.InfinispanProperties;
import org.hibernate.ogm.utils.TestHelper;

/**
 * Base class to test an expected exception and exception message,
 * risen in case of incompatible combination of:
 *
 * <li>Entity definition</li>
 * <li>Infinispan configuration</li>
 *
 * at Hibernate factory creation time.
 *
 */
public abstract class StartAndCloseInfinispanEmbeddedFactoryBaseTest {

	protected void startAndCloseFactory(Class<?> annotatedClass, String configFile) {
		Map<String, String> defaultTestSettings = TestHelper.getDefaultTestSettings();
		defaultTestSettings.put( InfinispanProperties.CONFIGURATION_RESOURCE_NAME, configFile );

		Properties properties = new Properties();
		properties.putAll( defaultTestSettings );

		Configuration configuration = new OgmConfiguration();
		configuration.addAnnotatedClass( annotatedClass ).addProperties( properties );

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		builder.applySettings( configuration.getProperties() );
		StandardServiceRegistry serviceRegistry = builder.build();

		SessionFactory sessionFactory = null;
		try {
			sessionFactory = configuration.buildSessionFactory( serviceRegistry );
			fail( "expected exception at Hibernate factory creation time was not raised" );
		}
		finally {
			if ( sessionFactory != null ) {
				sessionFactory.close();
			}
		}
	}

}
