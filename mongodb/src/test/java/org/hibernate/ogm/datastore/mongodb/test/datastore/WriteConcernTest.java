/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.impl.configuration.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBGlobalContext;
import org.hibernate.ogm.options.navigation.impl.AppendableConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.ConfigurationOptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.ProgrammaticOptionValueSource;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.WriteConcern;

/**
 * Test for the write concern setting.
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 * @author Gunnar Morling
 */
public class WriteConcernTest {

	private Map<String, Object> cfg;
	private MongoDBGlobalContext configuration;
	private ConfigurationPropertyReader reader;
	private AppendableConfigurationContext context;

	@Before
	public void setupConfigurationMapAndContexts() {
		cfg = new HashMap<String, Object>();
		cfg.put( OgmProperties.DATABASE, "database" );

		context = new AppendableConfigurationContext();
		configuration = new MongoDB().getConfigurationBuilder( new ConfigurationContext( context ) );

		reader = new ConfigurationPropertyReader( cfg, new ClassLoaderServiceImpl() );
	}

	@Test
	public void shouldUseAcknowledgedByDefault() {
		MongoDBConfiguration config = new MongoDBConfiguration( reader, getGlobalOptions() );
		assertEquals( config.buildOptions().getWriteConcern(), WriteConcern.ACKNOWLEDGED );
	}

	@Test
	public void shouldApplyValueGivenViaProperties() {
		cfg.put( MongoDBProperties.WRITE_CONCERN, "JOURNALED" );

		MongoDBConfiguration config = new MongoDBConfiguration( reader, getGlobalOptions() );
		assertEquals( config.buildOptions().getWriteConcern(), WriteConcern.JOURNALED );
	}

	@Test
	public void shouldApplyValueGivenViaGlobalOptions() {
		configuration.writeConcern( WriteConcernType.FSYNCED );

		MongoDBConfiguration config = new MongoDBConfiguration( reader, getGlobalOptions() );
		assertEquals( config.buildOptions().getWriteConcern(), WriteConcern.FSYNCED );
	}

	@Test
	public void shouldApplyCustomWriteConcernType() {
		cfg.put( MongoDBProperties.WRITE_CONCERN, WriteConcernType.CUSTOM );
		cfg.put( MongoDBProperties.WRITE_CONCERN_TYPE, MultipleDataCenters.class );

		MongoDBConfiguration config = new MongoDBConfiguration( reader, getGlobalOptions() );
		assertEquals( config.buildOptions().getWriteConcern(), new MultipleDataCenters() );
	}

	@Test(expected = HibernateException.class )
	public void shouldRaiseErrorIfStrategyIsCUSTOMButNoTypeIsGiven() {
		cfg.put( MongoDBProperties.WRITE_CONCERN, WriteConcernType.CUSTOM );
		new MongoDBConfiguration( new ConfigurationPropertyReader( cfg ), getGlobalOptions() );
	}

	private OptionsContext getGlobalOptions() {
		List<OptionValueSource> sources = Arrays.<OptionValueSource>asList(
				new ProgrammaticOptionValueSource( context ),
				new ConfigurationOptionValueSource( reader  )
		);

		return OptionsContextImpl.forGlobal( sources );
	}

	@SuppressWarnings("serial")
	public static class MultipleDataCenters extends com.mongodb.WriteConcern {

		public MultipleDataCenters() {
			super( "MultipleDataCenters", 0, false, true, false );
		}
	}
}
