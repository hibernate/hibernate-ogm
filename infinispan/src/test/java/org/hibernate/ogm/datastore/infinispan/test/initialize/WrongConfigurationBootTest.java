/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012-2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
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
