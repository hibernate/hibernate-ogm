/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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

import org.hibernate.ogm.datastore.infinispan.InfinispanProperties;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.StringHelper;

/**
 * Configuration for {@link org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider}.
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class InfinispanConfiguration {

	private static final Log log = LoggerFactory.make();

	private static final String INFINISPAN_DEFAULT_CONFIG = "org/hibernate/ogm/datastore/infinispan/default-config.xml";

	private String configName;
	private String jndi;

	/**
	 * @see org.hibernate.ogm.datastore.infinispan.Infinispan#CONFIGURATION_RESOURCE_NAME
	 * @return might be the name of the file (too look it up in the class path) or an URL to a file.
	 */
	public String getConfigurationName() {
		return configName;
	}

	/**
	 * @see org.hibernate.ogm.datastore.infinispan.Infinispan#CACHE_MANAGER_JNDI_NAME
	 * @return the {@literal JNDI} name of the cache manager
	 */
	public String getJndiName() {
		return jndi;
	}

	/**
	 * Initialize the internal values form the given {@link Map}.
	 *
	 * @param configurationMap
	 *            The values to use as configuration
	 */
	public void initConfiguration(Map configurationMap) {
		this.jndi = (String) configurationMap.get( InfinispanProperties.CACHE_MANAGER_JNDI_NAME );

		this.configName = (String) configurationMap.get( InfinispanProperties.CONFIGURATION_RESOURCE_NAME );
		if ( StringHelper.isEmpty( configName ) ) {
			this.configName = INFINISPAN_DEFAULT_CONFIG;
		}

		log.tracef( "Initializing Infinispan from configuration file at %1$s", configName );
	}
}
