/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.schema.impl;

import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.tool.schema.spi.SchemaManagementTool;

/**
 * @author Gunnar Morling
 *
 */
public class NoSqlSchemaManagementToolInitiator implements SessionFactoryServiceInitiator<SchemaManagementTool> {

	public static final NoSqlSchemaManagementToolInitiator INSTANCE = new NoSqlSchemaManagementToolInitiator();

	@Override
	public SchemaManagementTool initiateService(SessionFactoryImplementor sessionFactory, SessionFactoryOptions sessionFactoryOptions,
			ServiceRegistryImplementor registry) {

		final Object setting = registry.getService( ConfigurationService.class ).getSettings().get( AvailableSettings.SCHEMA_MANAGEMENT_TOOL );
		SchemaManagementTool tool = registry.getService( StrategySelector.class ).resolveStrategy( SchemaManagementTool.class, setting );
		if ( tool == null ) {
			tool = new NoSqlSchemaManagementTool( sessionFactory );
		}

		return tool;
	}

	@Override
	public Class<SchemaManagementTool> getServiceInitiated() {
		return SchemaManagementTool.class;
	}
}
