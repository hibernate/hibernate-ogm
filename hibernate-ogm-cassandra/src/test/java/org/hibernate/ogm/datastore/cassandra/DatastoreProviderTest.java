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

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.cassandra.impl.CassandraDatastoreProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class DatastoreProviderTest {
	
	Map<String, String> settings;
	CassandraDatastoreProvider provider;

	@Before
	public void setup() {
		this.provider = new CassandraDatastoreProvider();
		this.settings = new HashMap<String, String>();

		Properties properties = Environment.getProperties();
		
		for (String key : properties.stringPropertyNames()) {
			this.settings.put( key, properties.getProperty( key ) );
		}
	}

	@Test( expected = IllegalArgumentException.class )
	public void getConfigurationShouldThrowExceptionOnInvalidSetting() {
		this.provider.configure( null );
	}

	@Test
	public void defaultUrlValueShouldSuccessOnConfiguration() {
		this.settings.remove( "hibernate.ogm.cassandra.url" );
		this.provider.configure( this.settings );
	}

	@Test
	public void setupShouldSuccessOnRightConfiguration() {
		this.provider.configure( this.settings );
	}

	@Test(expected = HibernateException.class)
	public void startShouldFailWhenKeyspaceIsNotCreated() {
		this.settings.remove( OgmConfiguration.HIBERNATE_OGM_GENERATE_SCHEMA );
		this.provider.configure( this.settings );
		this.provider.start();
	}

	@Test(expected = HibernateException.class)
	public void startShouldFailOnWrongGenerateConfigurationIfKeyspaceDoesNotExist() {
		this.settings.put( OgmConfiguration.HIBERNATE_OGM_GENERATE_SCHEMA,
						   "wrong-value" );
		this.provider.configure( this.settings );
		this.provider.start();
	}

	@Test
	public void startAndStopShouldCreateKeyspaces() {
		this.settings.put( OgmConfiguration.HIBERNATE_OGM_GENERATE_SCHEMA,
						   OgmConfiguration.GenerateSchemaValue.CREATE_DROP.getValue() );
		this.provider.configure( this.settings );
		this.provider.start();
		this.provider.stop();
	}
}
