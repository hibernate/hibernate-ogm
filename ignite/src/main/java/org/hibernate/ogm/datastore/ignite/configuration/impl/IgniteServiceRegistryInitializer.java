/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.configuration.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.ignite.jpa.impl.IgniteOgmPersisterClassResolver;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Applies Ignite-specific configurations to bootstrapped service registries:
 * <ul>
 * <li>Registers Ignite's service initiators</li>
 * <li>Configures Hibernate Search so it works for Ignite</li>
 * </ul>
 * @author Dmitriy Kozlov
 * @author Victor Kadachigov
 */
public class IgniteServiceRegistryInitializer implements ServiceContributor {

	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		Map<Object, Object> settings = serviceRegistryBuilder.getSettings();
		boolean isOgmEnabled = isOgmEnabled( settings );

		if ( !isOgmEnabled ) {
			return;
		}

		serviceRegistryBuilder.addService( PersisterClassResolver.class, new IgniteOgmPersisterClassResolver() );
	}

	private boolean isOgmEnabled(Map<?, ?> settings) {
		return new ConfigurationPropertyReader( settings )
			.property( OgmProperties.ENABLED, boolean.class )
			.withDefault( false )
			.getValue();
	}
}
