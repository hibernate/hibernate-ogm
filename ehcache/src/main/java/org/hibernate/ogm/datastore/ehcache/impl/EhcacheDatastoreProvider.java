/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.impl;

import java.util.Map;
import java.util.Properties;

import javax.transaction.TransactionManager;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.ehcache.EhcacheDialect;
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableKey;
import org.hibernate.ogm.datastore.ehcache.impl.configuration.EhcacheConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.datastore.spi.DefaultDatastoreNames;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.service.impl.LuceneBasedQueryParserService;
import org.hibernate.ogm.service.impl.QueryParserService;
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

	private JtaPlatform jtaPlatform;
	private CacheManager cacheManager;
	private Cache<SerializableKey> entityCache;
	private Cache<SerializableKey> associationCache;
	private Cache<SerializableKey> identifierCache;

	private final EhcacheConfiguration config = new EhcacheConfiguration();

	@Override
	public void configure(Map map) {
		this.config.initialize( map );
	}

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return EhcacheDialect.class;
	}

	@Override
	public Class<? extends QueryParserService> getDefaultQueryParserServiceType() {
		return LuceneBasedQueryParserService.class;
	}

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistryImplementor) {
		this.jtaPlatform = serviceRegistryImplementor.getService( JtaPlatform.class );
	}

	@Override
	public void start() {
		final Configuration configuration = ConfigurationFactory.parseConfiguration( config.getUrl() );
		if ( jtaPlatform != null ) {
			OgmTransactionManagerLookupDelegate.transactionManager = jtaPlatform.retrieveTransactionManager();
			final FactoryConfiguration transactionManagerLookupParameter = new FactoryConfiguration();
			transactionManagerLookupParameter.setClass( OgmTransactionManagerLookupDelegate.class.getName() );
			configuration.addTransactionManagerLookup( transactionManagerLookupParameter );
		}
		cacheManager = CacheManager.create( config.getUrl() );

		entityCache = new Cache<SerializableKey>( cacheManager.getCache( DefaultDatastoreNames.ENTITY_STORE ) );
		associationCache = new Cache<SerializableKey>( cacheManager.getCache( DefaultDatastoreNames.ASSOCIATION_STORE ) );
		identifierCache = new Cache<SerializableKey>( cacheManager.getCache( DefaultDatastoreNames.IDENTIFIER_STORE ) );
	}

	@Override
	public void stop() {
		cacheManager.shutdown();
	}

	public Cache<SerializableKey> getEntityCache() {
		return entityCache;
	}

	public Cache<SerializableKey> getAssociationCache() {
		return associationCache;
	}

	public Cache<SerializableKey> getIdentifierCache() {
		return identifierCache;
	}

	public static class OgmTransactionManagerLookupDelegate implements TransactionManagerLookup {

		private static TransactionManager transactionManager;

		@Override
		public TransactionManager getTransactionManager() {
			return transactionManager;
		}

		@Override
		public void init() {
		}

		@Override
		public void register(EhcacheXAResource resource, boolean forRecovery) {
			// noop
		}

		@Override
		public void unregister(EhcacheXAResource resource, boolean forRecovery) {
			// noop
		}

		@Override
		public void setProperties(Properties properties) {
			// noop
		}
	}
}
