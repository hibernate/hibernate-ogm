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

import java.net.URL;
import java.util.Map;

import org.hibernate.ogm.datastore.infinispan.InfinispanProperties;
import org.hibernate.ogm.util.configurationreader.impl.ConfigurationPropertyReader;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Configuration for {@link org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider}.
 *
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class InfinispanConfiguration {

	private static final Log log = LoggerFactory.make();

	private static final String INFINISPAN_DEFAULT_CONFIG = "org/hibernate/ogm/datastore/infinispan/default-config.xml";

	private URL configUrl;
	private String jndi;

	/**
<<<<<<< HEAD:infinispan/src/main/java/org/hibernate/ogm/datastore/infinispan/impl/configuration/InfinispanConfiguration.java
	 * @see InfinispanProperties#CONFIGURATION_RESOURCE_NAME
	 * @return an URL identifying an Infinispan configuration file
=======
	 * @see org.hibernate.ogm.datastore.infinispan.Infinispan#CONFIGURATION_RESOURCENAME
	 * @return might be the name of the file (too look it up in the class path) or an URL to a file.
>>>>>>> OGM-292 Backend modules: Moving everything to org.hibernate.ogm.dialect.<%dialect%>.*:infinispan/src/main/java/org/hibernate/ogm/dialect/infinispan/datastore/impl/configuration/InfinispanConfiguration.java
	 */
	public URL getConfigurationUrl() {
		return configUrl;
	}

	/**
<<<<<<< HEAD:infinispan/src/main/java/org/hibernate/ogm/datastore/infinispan/impl/configuration/InfinispanConfiguration.java
	 * @see org.hibernate.ogm.datastore.infinispan.Infinispan#CACHE_MANAGER_JNDI_NAME
=======
	 * @see org.hibernate.ogm.datastore.infinispan.Infinispan#CACHE_MANAGER_RESOURCE_PROP
>>>>>>> OGM-292 Backend modules: Moving everything to org.hibernate.ogm.dialect.<%dialect%>.*:infinispan/src/main/java/org/hibernate/ogm/dialect/infinispan/datastore/impl/configuration/InfinispanConfiguration.java
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
		ConfigurationPropertyReader propertyReader = new ConfigurationPropertyReader( configurationMap );

		this.configUrl = propertyReader
				.property( InfinispanProperties.CONFIGURATION_RESOURCE_NAME, URL.class )
				.withDefault( InfinispanConfiguration.class.getClassLoader().getResource( INFINISPAN_DEFAULT_CONFIG ) )
				.getValue();

		this.jndi = propertyReader
				.property( InfinispanProperties.CACHE_MANAGER_JNDI_NAME, String.class )
				.getValue();

		log.tracef( "Initializing Infinispan from configuration file at %1$s", configUrl );
	}
}
