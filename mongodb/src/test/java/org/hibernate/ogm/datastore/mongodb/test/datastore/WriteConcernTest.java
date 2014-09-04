/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.ogm.options.navigation.impl.ConfigurationContextImpl;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.ConfigurationOptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.ProgrammaticOptionValueSource;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.WriteConcern;

/**
 * Test for the write concern setting.
 *
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
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
		configuration = new MongoDB().getConfigurationBuilder( new ConfigurationContextImpl( context ) );

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
