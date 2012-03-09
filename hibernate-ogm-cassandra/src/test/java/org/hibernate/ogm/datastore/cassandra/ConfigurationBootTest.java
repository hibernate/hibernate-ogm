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
package org.hibernate.ogm.datastore.cassandra;

import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.test.utils.CassandraTestHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Khanh Tuong Maudoux
 */
public class ConfigurationBootTest {

	Properties properties;

	@Before
	public void setup() {
		this.properties = Environment.getProperties();
	}

	@Test
	@Ignore
	public void testSimpleCassandraInitialization() {
		tryBoot();
	}

	private void tryBoot() {
		Configuration cfg = new OgmConfiguration();
		cfg.setProperties( properties );
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		if ( sessionFactory != null ) {
			try {
				// trigger service initialization, and also verifies it actually uses Cassandra:
				CassandraTestHelper.getProvider( sessionFactory );
			}
			finally {
				sessionFactory.close();
			}
		}
	}
}
