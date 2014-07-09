/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.initialize;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.infinispan.InfinispanProperties;
import org.hibernate.ogm.datastore.infinispan.utils.InfinispanTestHelper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Verify we provide useful information in case Infinispan is not starting correctly.
 *
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2012 Red Hat Inc.
 */
public class WrongConfigurationBootTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testSimpleInfinispanInitialization() {
		tryBoot( "infinispan-local.xml" );
	}

	@Test
	public void testIllegalInfinispanConfigurationReported() throws Throwable {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "Invalid URL given for configuration property '" + InfinispanProperties.CONFIGURATION_RESOURCE_NAME + "': does-not-exist-configuration-file.xml; The specified resource could not be found." );

		try {
			tryBoot( "does-not-exist-configuration-file.xml" );
		}
		catch (Exception e) {
			throw e.getCause();
		}
	}

	/**
	 * @param configurationResourceName
	 *            The Infinispan configuration resource to use to try booting OGM
	 */
	private void tryBoot(String configurationResourceName) {
		Configuration cfg = new OgmConfiguration();
		cfg.setProperty( OgmProperties.DATASTORE_PROVIDER, "infinispan" );
		cfg.setProperty( InfinispanProperties.CONFIGURATION_RESOURCE_NAME, configurationResourceName );
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		if ( sessionFactory != null ) {
			try {
				// trigger service initialization, and also verifies it actually uses Infinispan:
				InfinispanTestHelper.getProvider( sessionFactory );
			}
			finally {
				sessionFactory.close();
			}
		}
	}

}
