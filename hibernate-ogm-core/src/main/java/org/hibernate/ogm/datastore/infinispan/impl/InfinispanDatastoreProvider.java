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
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.DefaultDatastoreNames;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.infinispan.InfinispanDialect;
import org.hibernate.service.jndi.spi.JndiService;
import org.hibernate.service.spi.*;
import org.infinispan.Cache;
import org.infinispan.config.Configuration;
import org.infinispan.config.ConfigurationValidatingVisitor;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.config.InfinispanConfiguration;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import org.hibernate.HibernateException;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.util.impl.StringHelper;
import org.hibernate.service.jta.platform.spi.JtaPlatform;

/**
 * Provides access to Infinispan's CacheManager; one CacheManager is needed for all caches,
 * it can be taken via JNDI or started by this ServiceProvider; in this case it will also
 * be stopped when no longer needed.
 *
 * @author Sanne Grinovero
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class InfinispanDatastoreProvider implements DatastoreProvider, Startable, Stoppable,
													ServiceRegistryAwareService, Configurable {

	private JtaPlatform jtaPlatform;
	private JndiService jndiService;
	private Map cfg;
	private Map<String,Cache> caches;
	private boolean isCacheProvided;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return InfinispanDialect.class;
	}

	/**
	 * The configuration property to use as key to define a custom configuration for Infinispan.
	 */
	public static final String INFINISPAN_CONFIGURATION_RESOURCENAME = "hibernate.ogm.infinispan.configuration_resourcename";
	
	/**
	 * The key for the configuration property to define the jndi name of the cachemanager.
	 * If this property is defined, the cachemanager will be looked up via JNDI.
	 * JNDI properties passed in the form <tt>hibernate.jndi.*</tt> are used to define the context properties.
	 */
	public static final String CACHE_MANAGER_RESOURCE_PROP = "hibernate.ogm.infinispan.cachemanager_jndiname";
	
	public static final String INFINISPAN_DEFAULT_CONFIG = "org/hibernate/ogm/datastore/infinispan/default-config.xml";
	
	private static final Log log = LoggerFactory.make();
	
	private EmbeddedCacheManager cacheManager;

	public void start() {
		try {
			String jndiProperty = (String) cfg.get( CACHE_MANAGER_RESOURCE_PROP );
			if ( jndiProperty == null ) {
				String cfgName = (String) cfg.get( INFINISPAN_CONFIGURATION_RESOURCENAME );
				if ( StringHelper.isEmpty( cfgName ) ) {
					cfgName = INFINISPAN_DEFAULT_CONFIG;
				}
				log.tracef("Initializing Infinispan from configuration file at %1$s", cfgName);
				cacheManager = createCustomCacheManager( cfgName, jtaPlatform );
				isCacheProvided = false;
			}
			else {
				log.tracef("Retrieving Infinispan from JNDI at %1$s", jndiProperty);
				cacheManager = (EmbeddedCacheManager) jndiService.locate(jndiProperty);
				isCacheProvided = true;
			}
		}
		catch (RuntimeException e) {
			log.unableToInitializeInfinispan(e);
		}
		eagerlyInitializeCaches(cacheManager);
		//clear resources
		this.jtaPlatform = null;
		this.jndiService = null;
		this.cfg = null;
	}

	/**
	 * Need to make sure all needed caches are started before state transfer happens.
	 * This prevents this node to return undefined cache errors during replication
	 * when other nodes join this one.
	 * @param cacheManager
	 */
	private void eagerlyInitializeCaches(EmbeddedCacheManager cacheManager) {
		caches = new ConcurrentHashMap<String, Cache> (3);
		putInLocalCache(cacheManager, DefaultDatastoreNames.ASSOCIATION_STORE);
		putInLocalCache(cacheManager, DefaultDatastoreNames.ENTITY_STORE);
		putInLocalCache(cacheManager, DefaultDatastoreNames.IDENTIFIER_STORE);
	}

	private void putInLocalCache(EmbeddedCacheManager cacheManager, String cacheName) {
		caches.put(
				cacheName,
				cacheManager.getCache(cacheName)
		);
	}

	private EmbeddedCacheManager createCustomCacheManager(String cfgName, JtaPlatform platform) {
		try {
			InfinispanConfiguration configuration = InfinispanConfiguration.newInfinispanConfiguration(
					cfgName, InfinispanConfiguration.resolveSchemaPath(),
					new ConfigurationValidatingVisitor(),
					Thread.currentThread().getContextClassLoader() );
			GlobalConfiguration globalConfiguration = configuration.parseGlobalConfiguration();
			Configuration defaultConfiguration = configuration.parseDefaultConfiguration();
			TransactionManagerLookupDelegator transactionManagerLookupDelegator = new TransactionManagerLookupDelegator( platform );
			final DefaultCacheManager cacheManager = new DefaultCacheManager( globalConfiguration, defaultConfiguration, true );
			for (Map.Entry<String, Configuration> entry : configuration.parseNamedConfigurations().entrySet()) {
				Configuration cfg = entry.getValue();
				if ( transactionManagerLookupDelegator.isValid() ) {
					cfg.fluent().transactionManagerLookup(transactionManagerLookupDelegator);
				}
				cacheManager.defineConfiguration( entry.getKey(), cfg );
			}
			cacheManager.start();
			return cacheManager;
		} catch (RuntimeException re) {
			raiseConfigurationError(re, cfgName);
		}
		catch (IOException e) {
			raiseConfigurationError(e, cfgName);
		}
		return null; //actually this line is unreachable
	}

	public EmbeddedCacheManager getEmbeddedCacheManager() {
		return cacheManager;
	}

	//prefer generic form over specific ones to prepare for flexible cache setting
	public Cache getCache(String name) {
		return caches.get(name);
	}

	public void stop() {
		if ( !isCacheProvided && cacheManager != null ) {
			cacheManager.stop();
		}
	}

	private void raiseConfigurationError(Exception e, String cfgName) {
		throw new HibernateException(
				"Could not start Infinispan CacheManager using as configuration file: " + cfgName, e
		);
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		jtaPlatform = serviceRegistry.getService( JtaPlatform.class );
		jndiService = serviceRegistry.getService( JndiService.class );
	}

	@Override
	public void configure(Map configurationValues) {
		cfg = configurationValues;
	}
}
