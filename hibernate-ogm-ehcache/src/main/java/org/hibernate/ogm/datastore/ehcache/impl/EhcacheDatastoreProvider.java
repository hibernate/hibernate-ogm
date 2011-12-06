package org.hibernate.ogm.datastore.ehcache.impl;

import java.util.Map;

import net.sf.ehcache.CacheManager;

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
		// TODO Bootstrap CacheManager accordingly, including using the proper JTA TxManager
		cacheManager = CacheManager.create();
	}

	@Override
	public void stop() {
		cacheManager.shutdown();
	}

	public CacheManager getCacheManager() {
		// Might what to use a getCache(String): Cache here instead
		return cacheManager;
	}
}
