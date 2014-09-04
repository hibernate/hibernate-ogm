/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.options.navigation.source.impl;

import java.util.Arrays;
import java.util.List;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.OptionConfigurator;
import org.hibernate.ogm.cfg.impl.ConfigurableImpl;
import org.hibernate.ogm.cfg.impl.InternalProperties;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.impl.AppendableConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Provides the list of {@link OptionValueSource}s to be considered by default when creating an
 * {@link OptionsContextImpl}.
 *
 * @author Gunnar Morling
 */
public class OptionValueSources {

	private static final Log log = LoggerFactory.make();

	private OptionValueSources() {
	}

	public static List<OptionValueSource> getDefaultSources(ConfigurationPropertyReader propertyReader) {
		AppendableConfigurationContext programmaticOptions = propertyReader.property( InternalProperties.OGM_OPTION_CONTEXT, AppendableConfigurationContext.class )
				.instantiate()
				.getValue();
		OptionConfigurator configurator = propertyReader.property( OgmProperties.OPTION_CONFIGURATOR, OptionConfigurator.class )
				.instantiate()
				.getValue();

		if ( configurator != null ) {
			if ( programmaticOptions != null ) {
				throw log.ambigiousOptionConfiguration( OgmProperties.OPTION_CONFIGURATOR );
			}

			programmaticOptions = invokeOptionConfigurator( configurator );
		}

		return programmaticOptions != null ?
				Arrays.<OptionValueSource>asList( new ProgrammaticOptionValueSource( programmaticOptions ), new AnnotationOptionValueSource(), new ConfigurationOptionValueSource( propertyReader ) ) :
				Arrays.<OptionValueSource>asList( new AnnotationOptionValueSource(), new ConfigurationOptionValueSource( propertyReader ) );
	}

	/**
	 * Invokes the given configurator, obtaining the correct global context type via the datastore configuration type of
	 * the current datastore provider.
	 *
	 * @param configurator the configurator to invoke
	 * @return a context object containing the options set via the given configurator
	 */
	private static <D extends DatastoreConfiguration<G>, G extends GlobalContext<?, ?>> AppendableConfigurationContext invokeOptionConfigurator(
			OptionConfigurator configurator) {

		ConfigurableImpl configurable = new ConfigurableImpl();
		configurator.configure( configurable );
		return configurable.getContext();
	}
}
