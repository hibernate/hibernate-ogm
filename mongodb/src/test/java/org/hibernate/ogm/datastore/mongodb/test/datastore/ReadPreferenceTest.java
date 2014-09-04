/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.mongodb.test.datastore;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.ogm.cfg.Configurable;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.OptionConfigurator;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.MongoDBProperties;
import org.hibernate.ogm.datastore.mongodb.impl.configuration.MongoDBConfiguration;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSources;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
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
