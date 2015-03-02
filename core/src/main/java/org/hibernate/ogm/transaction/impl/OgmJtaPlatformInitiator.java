/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.transaction.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmJtaPlatformInitiator implements StandardServiceInitiator<JtaPlatform> {
	public static final OgmJtaPlatformInitiator INSTANCE = new OgmJtaPlatformInitiator();

	@Override
	public Class<JtaPlatform> getServiceInitiated() {
		return JtaPlatform.class;
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public JtaPlatform initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		if ( hasExplicitPlatform( configurationValues ) ) {
			return JtaPlatformInitiator.INSTANCE.initiateService( configurationValues, registry );
		}

		if ( isNeo4j( registry.getService( DatastoreProvider.class ) ) ) {
			configurationValues.put( Environment.JTA_PLATFORM, "org.hibernate.ogm.datastore.neo4j.transaction.impl.Neo4jJtaPlatform" );
			return JtaPlatformInitiator.INSTANCE.initiateService( configurationValues, registry );
		}
		return new JBossStandAloneJtaPlatform();
	}

	private boolean isNeo4j(DatastoreProvider datastoreProvider) {
		return AvailableDatastoreProvider.NEO4J_EMBEDDED.getDatastoreProviderClassName()
				.equals( datastoreProvider.getClass().getName() );
	}

	private boolean hasExplicitPlatform(Map configVales) {
		return configVales.containsKey( AvailableSettings.JTA_PLATFORM );
	}
}
