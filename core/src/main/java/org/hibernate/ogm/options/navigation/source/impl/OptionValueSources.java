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
package org.hibernate.ogm.options.navigation.source.impl;

import java.util.Arrays;
import java.util.List;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.impl.ConfigurableImpl;
import org.hibernate.ogm.cfg.impl.InternalProperties;
import org.hibernate.ogm.cfg.spi.OptionConfigurator;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.options.navigation.impl.AppendableConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.OptionsContextImpl;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;
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
