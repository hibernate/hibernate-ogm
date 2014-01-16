/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.transaction.impl;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.datastore.impl.AvailableDatastoreProvider;
import org.hibernate.ogm.service.impl.OptionalServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmJtaPlatformInitiator extends OptionalServiceInitiator<JtaPlatform> {
	public static final OgmJtaPlatformInitiator INSTANCE = new OgmJtaPlatformInitiator();

	@Override
	public Class<JtaPlatform> getServiceInitiated() {
		return JtaPlatform.class;
	}

	@Override
	protected JtaPlatform buildServiceInstance(Map configurationValues, ServiceRegistryImplementor registry) {
		if ( hasExplicitPlatform( configurationValues ) ) {
			return JtaPlatformInitiator.INSTANCE.initiateService( configurationValues, registry );
		}
		if ( isNeo4j( configurationValues ) ) {
			configurationValues.put( Environment.JTA_PLATFORM, "org.hibernate.ogm.transaction.neo4j.impl.Neo4jJtaPlatform" );
			return JtaPlatformInitiator.INSTANCE.initiateService( configurationValues, registry );
		}
		return new JBossStandAloneJtaPlatform();
	}

	//TODO get rid of this!!!
	private boolean isNeo4j(Map configuration) {
		String propertyValue = (String) configuration.get( OgmProperties.DATASTORE_PROVIDER );

		if ( AvailableDatastoreProvider.isShortName( propertyValue ) ) {
			return AvailableDatastoreProvider.byShortName( propertyValue ) == AvailableDatastoreProvider.NEO4J_EMBEDDED;
		}
		else {
			return AvailableDatastoreProvider.NEO4J_EMBEDDED.getDatastoreProviderClassName().equals( propertyValue );
		}
	}

	@Override
	protected StandardServiceInitiator<JtaPlatform> backupInitiator() {
		return JtaPlatformInitiator.INSTANCE;
	}

	private boolean hasExplicitPlatform(Map configVales) {
		return configVales.containsKey( AvailableSettings.JTA_PLATFORM );
	}
}
