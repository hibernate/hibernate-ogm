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
package org.hibernate.ogm.datastore.infinispan.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.infinispan.config.Configuration;
import org.infinispan.config.ConfigurationValidatingVisitor;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.config.InfinispanConfiguration;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import org.hibernate.HibernateException;

/**
 * Provides access to Infinispan's CacheManager; one CacheManager is needed for all caches,
 * it can be taken via JNDI or started by this ServiceProvider; in this case it will also
 * be stopped when no longer needed.
 *
 * @author Sanne Grinovero
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
//TODO extract an interface for other datastores
public class CacheManagerServiceProvider {

	/**
	 * The configuration property to use as key to define a custom configuration for Infinispan.
	 */
	public static final String INFINISPAN_CONFIGURATION_RESOURCENAME = "hibernate.ogm.infinispan.configuration_resourcename";
	public static final String INFINISPAN_DEFAULT_CONFIG = "org/hibernate/ogm/datastore/infinispan/default-config.xml";
	private EmbeddedCacheManager cacheManager;

	public void start(Properties cfg) {
		String cfgName = cfg.getProperty(
				INFINISPAN_CONFIGURATION_RESOURCENAME,
				INFINISPAN_DEFAULT_CONFIG
		);
		cacheManager = createCustomCacheManager(cfgName, cfg);
	}

	private EmbeddedCacheManager createCustomCacheManager(String cfgName, Properties properties) {
		try {
			InfinispanConfiguration configuration = InfinispanConfiguration.newInfinispanConfiguration(
					cfgName, InfinispanConfiguration.resolveSchemaPath(),
					new ConfigurationValidatingVisitor());
			GlobalConfiguration globalConfiguration = configuration.parseGlobalConfiguration();
			Configuration defaultConfiguration = configuration.parseDefaultConfiguration();
			TransactionManagerLookupDelegator transactionManagerLookupDelegator = new TransactionManagerLookupDelegator( properties );
			final DefaultCacheManager cacheManager = new DefaultCacheManager( globalConfiguration, defaultConfiguration, true );
			for (Map.Entry<String, Configuration> entry : configuration.parseNamedConfigurations().entrySet()) {
				Configuration cfg = entry.getValue();
				if ( transactionManagerLookupDelegator.isValid() ) {
					cfg.setTransactionManagerLookup( transactionManagerLookupDelegator );
				}
				cacheManager.defineConfiguration( entry.getKey(), cfg );
			}
			return cacheManager;
		} catch (RuntimeException re) {
			raiseConfigurationError(re, cfgName);
		}
		catch (IOException e) {
			raiseConfigurationError(e, cfgName);
		}
		return null; //actually this line is unreachable
	}

	public EmbeddedCacheManager getService() {
		return cacheManager;
	}

	public void stop() {
		if ( cacheManager != null ) {
			cacheManager.stop();
		}
	}

	private void raiseConfigurationError(Exception e, String cfgName) {
		throw new HibernateException(
				"Could not start Infinispan CacheManager using as configuration file: " + cfgName, e
		);
	}
}
