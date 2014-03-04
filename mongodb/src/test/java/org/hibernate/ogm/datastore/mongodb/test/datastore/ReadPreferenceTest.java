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
import org.hibernate.ogm.cfg.Configurable;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.spi.OptionConfigurator;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.impl.configuration.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSources;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;
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
	private OptionsContext globalOptions;
	private ConfigurationPropertyReader reader;

	@Before
	public void setupConfigurationMapAndContexts() {
		cfg = new HashMap<String, Object>();
		cfg.put( OgmProperties.DATABASE, "database" );

		reader = new ConfigurationPropertyReader( cfg, new ClassLoaderServiceImpl() );
		globalOptions = OptionsContextImpl.forGlobal( OptionValueSources.getDefaultSources( reader ) );
	}

	@Test
	public void shouldUsePrimaryByDefault() {
		MongoDBConfiguration config = new MongoDBConfiguration( reader, globalOptions );
		assertEquals( config.buildOptions().getReadPreference(), ReadPreference.primary() );
	}

	@Test
	public void shouldApplyValueGivenViaProperties() {
		cfg.put( MongoDBProperties.READ_PREFERENCE, "SECONDARY" );

		MongoDBConfiguration config = new MongoDBConfiguration( reader, globalOptions );
		assertEquals( config.buildOptions().getReadPreference(), ReadPreference.secondary() );
	}

	@Test
	public void shouldApplyValueGivenViaGlobalOptions() {
		cfg.put( OgmProperties.OPTION_CONFIGURATOR, new OptionConfigurator() {
			@Override
			public void configure(Configurable configurable) {
				configurable.configureOptionsFor( MongoDB.class ).readPreference( ReadPreferenceType.SECONDARY_PREFERRED );
			}
		} );

		globalOptions = OptionsContextImpl.forGlobal( OptionValueSources.getDefaultSources( reader ) );

		MongoDBConfiguration config = new MongoDBConfiguration( reader, globalOptions );
		assertEquals( config.buildOptions().getReadPreference(), ReadPreference.secondaryPreferred() );
	}
}
