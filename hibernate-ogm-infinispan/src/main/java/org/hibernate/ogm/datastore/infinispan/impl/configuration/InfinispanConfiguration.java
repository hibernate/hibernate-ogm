/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.infinispan.impl.configuration;

import java.util.Map;

import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * It contains all the configuration properties required to instanciate the {@see InfinispanDatastoreProvider}
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class InfinispanConfiguration {

	private static final Log log = LoggerFactory.make();

	private String configName;
	private String jndi;

	public String getConfigName() {
		return configName;
	}

	public String getJndi() {
		return jndi;
	}

	/**
	 * Initialize all the configuration properties used by the datastore provider
	 *
	 * @param configurationMap all the properties from the configuration files and the environment
	 */
	public void initConfiguration(Map configurationMap) {
		this.jndi = (String) configurationMap.get( Environment.CACHE_MANAGER_RESOURCE_PROP );

		this.configName = (String) configurationMap.get( Environment.INFINISPAN_CONFIGURATION_RESOURCENAME );
		if ( StringHelper.isEmpty( configName ) ) {
			this.configName = Environment.INFINISPAN_DEFAULT_CONFIG;
		}

		log.tracef( "Initializing Infinispan from configuration file at %1$s", configName );
	}
}
