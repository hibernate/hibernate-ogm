/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.ehcache.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.transaction.TransactionManager;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.ehcache.EhcacheDialect;
import org.hibernate.service.jta.platform.spi.JtaPlatform;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * @author Alex Snaps
 */
public class EhcacheDatastoreProvider implements DatastoreProvider, Startable, Stoppable,
		ServiceRegistryAwareService, Configurable {

	public static final String EHCACHE_CONFIG = "hibernate.ogm.ehcache.configuration_resourcename";

	private Map cfg;
	private JtaPlatform jtaPlatform;
	private CacheManager cacheManager;

	@Override
	public void configure(Map map) {
		this.cfg = map;
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return EhcacheDialect.class;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistryImplementor) {
		this.jtaPlatform = serviceRegistryImplementor.getService( JtaPlatform.class );
	}

	@Override
	public void start() {
		final String configUrl = (String) this.cfg.get( EHCACHE_CONFIG );
		final URL url;
		if ( configUrl != null ) {
			try {
				url = new URL( configUrl );
			}
			catch ( MalformedURLException e ) {
				throw new RuntimeException( e );
			}
		}
		else {
			url = this.getClass().getResource( "/org/hibernate/ogm/datastore/ehcache/default-ehcache.xml" );
		}
		final Configuration configuration = ConfigurationFactory.parseConfiguration( url );
		if ( jtaPlatform != null ) {
			OgmTransactionManagerLookupDelegate.transactionManager = jtaPlatform.retrieveTransactionManager();
			final FactoryConfiguration transactionManagerLookupParameter = new FactoryConfiguration();
			transactionManagerLookupParameter.setClass( OgmTransactionManagerLookupDelegate.class.getName() );
			configuration.addTransactionManagerLookup( transactionManagerLookupParameter );
		}
		cacheManager = CacheManager.create( url );
	}

	@Override
	public void stop() {
		cacheManager.shutdown();
	}

	public CacheManager getCacheManager() {
		// Might what to use a getCache(String): Cache here instead
		return cacheManager;
	}

	public static class OgmTransactionManagerLookupDelegate implements TransactionManagerLookup {

		private static TransactionManager transactionManager;

		@Override
		public TransactionManager getTransactionManager() {
			return transactionManager;
		}

		@Override
		public void register(EhcacheXAResource resource) {
			// noop
		}

		@Override
		public void unregister(EhcacheXAResource resource) {
			// noop
		}

		@Override
		public void setProperties(Properties properties) {
			// noop
		}
	}
}
