/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.initialize;

import java.io.FileNotFoundException;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.test.utils.InfinispanTestHelper;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Verify we provide useful information in case Infinispan is not starting correctly.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class WrongConfigurationBootTest {

	@Test
	public void testSimpleInfinispanInitialization() {
		tryBoot( "infinispan-local.xml" );
	}

	@Test
	public void testIllegalInfinispanConfigurationReported() {
		try {
			tryBoot( "does-not-exist-configuration-file.xml" );
			fail( "should have thrown an exception" );
		}
		catch ( HibernateException he ) {
			assertTrue( he.getMessage().contains( "Unable to find or initialize Infinispan CacheManager" ) );
			Throwable cause = he.getCause();
			assertTrue( cause.getMessage().contains( "Could not start Infinispan CacheManager using as configuration file: does-not-exist-configuration-file.xml" ) );
			Throwable originalCause = cause.getCause();
			assertTrue( originalCause.getMessage().contains( "does-not-exist-configuration-file.xml" ) );
			assertEquals( FileNotFoundException.class, originalCause.getClass() );
		}
	}

	/**
	 * @param configurationResourceName
	 *            The Infinispan configuration resource to use to try booting OGM
	 */
	private void tryBoot(String configurationResourceName) {
		Configuration cfg = new OgmConfiguration();
		cfg.setProperty( "hibernate.ogm.datastore.provider", "infinispan" );
		cfg.setProperty( "hibernate.ogm.infinispan.configuration_resourcename", configurationResourceName );
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
