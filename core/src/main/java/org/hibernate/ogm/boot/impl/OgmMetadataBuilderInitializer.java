/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.boot.impl;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataBuilderInitializer;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.ogm.boot.model.naming.impl.OgmImplicitNamingStrategy;
import org.hibernate.ogm.service.impl.OgmConfigurationService;

/**
 * Contributor for Hibernate OGM's modifications to bootstrapped {@link MetadataBuilder}s.
 * <p>
 * Registered and discovered by ORM through the service loader mechanism.
 *
 * @author Gunnar Morling
 */
public class OgmMetadataBuilderInitializer implements MetadataBuilderInitializer {

	@Override
	public void contribute(MetadataBuilder metadataBuilder, StandardServiceRegistry serviceRegistry) {
		if ( !serviceRegistry.getService( OgmConfigurationService.class ).isOgmEnabled() ) {
			return;
		}

		metadataBuilder.applyImplicitNamingStrategy( new OgmImplicitNamingStrategy() );

		boolean isUseNewGeneratorMappingsConfigured = serviceRegistry.getService( ConfigurationService.class )
				.getSettings()
				.containsKey( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS );

		if ( !isUseNewGeneratorMappingsConfigured ) {
			metadataBuilder.enableNewIdentifierGeneratorSupport( true );
		}
	}
}
