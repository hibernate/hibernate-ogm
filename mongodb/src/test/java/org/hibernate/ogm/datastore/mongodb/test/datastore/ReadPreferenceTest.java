/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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

package org.hibernate.ogm.datastore.mongodb.test.datastore;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.impl.configuration.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBGlobalContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.WritableOptionsServiceContext;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.ReadPreference;

/**
 * Test for the {@link MongoDBProperties#READ_PREFERENCE} setting.
 *
 * @author Gunnar Morling
 */
public class ReadPreferenceTest {

	private Map<String, Object> cfg;
	private WritableOptionsServiceContext optionsContext;
	private MongoDBGlobalContext configuration;

	@Before
	public void setupConfigurationMapAndContexts() {
		cfg = new HashMap<String, Object>();
		cfg.put( OgmProperties.DATABASE, "database" );

		optionsContext = new WritableOptionsServiceContext();
		configuration = new MongoDB().getConfigurationBuilder( new ConfigurationContext( optionsContext ) );
	}

	@Test
	public void shouldUsePrimaryByDefault() {
		MongoDBConfiguration config = new MongoDBConfiguration( cfg, OptionsContainer.EMPTY, new ClassLoaderServiceImpl() );
		assertEquals( config.buildOptions().getReadPreference(), ReadPreference.primary() );
	}

	@Test
	public void shouldApplyValueGivenViaProperties() {
		cfg.put( MongoDBProperties.READ_PREFERENCE, "SECONDARY" );

		MongoDBConfiguration config = new MongoDBConfiguration( cfg, OptionsContainer.EMPTY, new ClassLoaderServiceImpl() );
		assertEquals( config.buildOptions().getReadPreference(), ReadPreference.secondary() );
	}

	@Test
	public void shouldApplyValueGivenViaGlobalOptions() {
		configuration.readPreference( ReadPreferenceType.SECONDARY_PREFERRED );

		MongoDBConfiguration config = new MongoDBConfiguration( cfg, optionsContext.getGlobalOptions(), new ClassLoaderServiceImpl() );
		assertEquals( config.buildOptions().getReadPreference(), ReadPreference.secondaryPreferred() );
	}

	@Test
	public void shouldPreferValueGivenViaGlobalOptionsOverValueFromProperties() {
		cfg.put( MongoDBProperties.READ_PREFERENCE, "SECONDARY" );

		configuration.readPreference( ReadPreferenceType.SECONDARY_PREFERRED );

		MongoDBConfiguration config = new MongoDBConfiguration( cfg, optionsContext.getGlobalOptions(), new ClassLoaderServiceImpl() );
		assertEquals( config.buildOptions().getReadPreference(), ReadPreference.secondaryPreferred() );
	}
}
