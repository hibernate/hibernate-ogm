/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.boot.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.impl.HibernateSearchIntegration;
import org.hibernate.ogm.datastore.impl.DatastoreProviderInitiator;
import org.hibernate.ogm.dialect.eventstate.impl.EventContextManagerInitiator;
import org.hibernate.ogm.dialect.impl.GridDialectInitiator;
import org.hibernate.ogm.dialect.impl.IdentityColumnAwareGridDialectInitiator;
import org.hibernate.ogm.dialect.impl.MultigetGridDialectInitiator;
import org.hibernate.ogm.dialect.impl.OgmDialectFactoryInitiator;
import org.hibernate.ogm.dialect.impl.OptimisticLockingAwareGridDialectInitiator;
import org.hibernate.ogm.dialect.impl.QueryableGridDialectInitiator;
import org.hibernate.ogm.jdbc.impl.OgmConnectionProviderInitiator;
import org.hibernate.ogm.jpa.impl.OgmMutableIdentifierGeneratorFactoryInitiator;
import org.hibernate.ogm.jpa.impl.OgmPersisterClassResolverInitiator;
import org.hibernate.ogm.options.navigation.impl.OptionsServiceInitiator;
import org.hibernate.ogm.query.impl.OgmQueryTranslatorFactoryInitiator;
import org.hibernate.ogm.service.impl.OgmConfigurationService;
import org.hibernate.ogm.service.impl.OgmJdbcServicesInitiator;
import org.hibernate.ogm.service.impl.OgmSessionFactoryServiceRegistryFactoryInitiator;
import org.hibernate.ogm.transaction.impl.OgmJtaPlatformInitiator;
import org.hibernate.ogm.transaction.impl.OgmTransactionCoordinatorBuilderInitiator;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Applies OGM-specific configurations to bootstrapped service registries:
 * <ul>
 * <li>Registers OGM's service initiators</li>
 * <li>Configures Hibernate Search so it works for OGM</li>
 * </ul>
 *
 * @author Gunnar Morling
 */
public class OgmServiceRegistryInitializer implements ServiceContributor {

	@Override
	public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		Map<Object, Object> settings = serviceRegistryBuilder.getSettings();
		boolean isOgmEnabled = isOgmEnabled( settings );

		// registering OgmService in each case; Other OGM extensions then can recognize whether OGM is enabled or not
		// and either register their specific customizations or pass through the ORM default implementations
		serviceRegistryBuilder.addService( OgmConfigurationService.class, new OgmConfigurationServiceImpl( isOgmEnabled ) );

		if ( !isOgmEnabled ) {
			return;
		}

		if ( !settings.containsKey( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED ) ) {
			serviceRegistryBuilder.applySetting( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, false );
		}

		HibernateSearchIntegration.resetProperties( serviceRegistryBuilder );

		serviceRegistryBuilder.addInitiator( OgmQueryTranslatorFactoryInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmSessionFactoryServiceRegistryFactoryInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmPersisterClassResolverInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmConnectionProviderInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmDialectFactoryInitiator.INSTANCE);
		serviceRegistryBuilder.addInitiator( OgmTransactionCoordinatorBuilderInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmJtaPlatformInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmJdbcServicesInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( DatastoreProviderInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OptionsServiceInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OgmMutableIdentifierGeneratorFactoryInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( EventContextManagerInitiator.INSTANCE );

		serviceRegistryBuilder.addInitiator( GridDialectInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( QueryableGridDialectInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( IdentityColumnAwareGridDialectInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( OptimisticLockingAwareGridDialectInitiator.INSTANCE );
		serviceRegistryBuilder.addInitiator( MultigetGridDialectInitiator.INSTANCE );
	}

	private boolean isOgmEnabled(Map<?, ?> settings) {
		return new ConfigurationPropertyReader( settings )
			.property( OgmProperties.ENABLED, boolean.class )
			.withDefault( false )
			.getValue();
	}
}
