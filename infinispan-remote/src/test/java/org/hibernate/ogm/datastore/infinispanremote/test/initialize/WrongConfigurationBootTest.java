/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.initialize;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.infinispanremote.InfinispanRemoteProperties;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteTestHelper;
import org.hibernate.ogm.datastore.infinispanremote.utils.RemoteHotRodServerRule;
import org.hibernate.ogm.utils.TestHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Verify we provide useful information in case the configuration file is not found.
 *
 * @author Sanne Grinovero (C) 2016 Red Hat Inc.
 */
public class WrongConfigurationBootTest {

	@Rule
	public RemoteHotRodServerRule hotRodServer = new RemoteHotRodServerRule();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testHotrodConnectionEstablished() {
		tryBoot( "hotrod-client-testingconfiguration.properties" );
	}

	@Test
	public void testIllegalConfigurationReported() throws Throwable {
		final String configurationName = "does-not-exist-configuration-file.properties";
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "Invalid URL given for configuration property '"
				+ InfinispanRemoteProperties.CONFIGURATION_RESOURCE_NAME
				+ "': " + configurationName + "; The specified resource could not be found." );
		try {
			tryBoot( configurationName );
		}
		catch (Exception e) {
			throw e.getCause();
		}
	}

	/**
	 * @param configurationResourceName The Infinispan configuration resource to use to try booting OGM
	 */
	private void tryBoot(String configurationResourceName) {
		Map<String, Object> settings = new HashMap<>();
		settings.put( OgmProperties.DATASTORE_PROVIDER, "infinispan_remote" );
		settings.put( InfinispanRemoteProperties.CONFIGURATION_RESOURCE_NAME, configurationResourceName );

		SessionFactory sessionFactory = TestHelper.getDefaultTestSessionFactory( settings );

		if ( sessionFactory != null ) {
			try {
				// trigger service initialization, and also verifies it actually uses Infinispan:
				InfinispanRemoteTestHelper.getProvider( sessionFactory );
			}
			finally {
				sessionFactory.close();
			}
		}
	}

}
