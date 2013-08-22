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
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.ogm.datastore.infinispan.impl.configuration.InfinispanConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.DefaultDatastoreNames;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.infinispan.InfinispanDialect;
import org.hibernate.ogm.options.navigation.impl.NoSqlMappingFactory;
import org.hibernate.ogm.options.spi.MappingFactory;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.jndi.spi.JndiService;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.FileLookupFactory;

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

	private static final Log log = LoggerFactory.make();

	private JtaPlatform jtaPlatform;
	private JndiService jndiService;
	private Map<String,Cache> caches;
	private boolean isCacheProvided;
	private boolean started = false;
	private EmbeddedCacheManager cacheManager;
	private final InfinispanConfiguration config = new InfinispanConfiguration();

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return InfinispanDialect.class;
	}

	public void start() {
		if ( started ) {
			// ServiceRegistry might invoke start multiple times, but always from the same initialization thread.
			//TODO remove the start flag: no longer needed after HHH-7147
			return;
		}
		try {
			String jndiProperty = config.getJndiName();
			if ( jndiProperty == null ) {
				cacheManager = createCustomCacheManager( config.getConfigurationName(), jtaPlatform );
				isCacheProvided = false;
			}
			else {
				log.tracef( "Retrieving Infinispan from JNDI at %1$s", jndiProperty );
				cacheManager = (EmbeddedCacheManager) jndiService.locate( jndiProperty );
				isCacheProvided = true;
			}
		}
		catch (RuntimeException e) {
			throw log.unableToInitializeInfinispan( e );
		}
		eagerlyInitializeCaches( cacheManager );
		//clear resources
		this.jtaPlatform = null;
		this.jndiService = null;
		this.started = true;
	}

	/**
	 * Need to make sure all needed caches are started before state transfer happens.
	 * This prevents this node to return undefined cache errors during replication
	 * when other nodes join this one.
	 * @param cacheManager
	 */
	private void eagerlyInitializeCaches(EmbeddedCacheManager cacheManager) {
		caches = new ConcurrentHashMap<String, Cache>( 3 );
		putInLocalCache( cacheManager, DefaultDatastoreNames.ASSOCIATION_STORE );
		putInLocalCache( cacheManager, DefaultDatastoreNames.ENTITY_STORE );
		putInLocalCache( cacheManager, DefaultDatastoreNames.IDENTIFIER_STORE );
	}

	private void putInLocalCache(EmbeddedCacheManager cacheManager, String cacheName) {
		caches.put( cacheName, cacheManager.getCache( cacheName ) );
	}

	private EmbeddedCacheManager createCustomCacheManager(String cfgName, JtaPlatform platform) {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		TransactionManagerLookupDelegator transactionManagerLookupDelegator = new TransactionManagerLookupDelegator( platform );
		try {
			InputStream configurationFile = FileLookupFactory.newInstance().lookupFileStrict( cfgName, contextClassLoader );
			try {
				cacheManager = new DefaultCacheManager( configurationFile, false );
				// override the named cache configuration defined in the configuration file to
				// inject the platform TransactionManager
				for (String cacheName : cacheManager.getCacheNames() ) {
					Configuration originalCfg = cacheManager.getCacheConfiguration( cacheName );
					Configuration newCfg = new ConfigurationBuilder()
						.read( originalCfg )
							.transaction()
								.transactionManagerLookup( transactionManagerLookupDelegator )
						.build();
					cacheManager.defineConfiguration( cacheName, newCfg );
				}
				cacheManager.start();
				return cacheManager;
			}
			finally {
				if ( configurationFile != null ) {
					configurationFile.close();
				}
			}
		}
		catch ( RuntimeException re ) {
			throw raiseConfigurationError( re, cfgName );
		}
		catch (IOException e) {
			throw raiseConfigurationError( e, cfgName );
		}
	}

	public EmbeddedCacheManager getEmbeddedCacheManager() {
		return cacheManager;
	}

	//prefer generic form over specific ones to prepare for flexible cache setting
	public Cache getCache(String name) {
		return caches.get( name );
	}

	public void stop() {
		if ( !isCacheProvided && cacheManager != null ) {
			cacheManager.stop();
		}
	}

	private HibernateException raiseConfigurationError(Exception e, String cfgName) {
		return new HibernateException(
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
		this.config.initConfiguration( configurationValues );
	}

	@Override
	public Class<? extends MappingFactory<?>> getMappingFactoryType() {
		return NoSqlMappingFactory.class;
	}
}
