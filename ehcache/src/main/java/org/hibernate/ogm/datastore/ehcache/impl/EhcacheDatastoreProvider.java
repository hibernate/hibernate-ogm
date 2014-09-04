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
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableAssociationKey;
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableEntityKey;
import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableIdSourceKey;
import org.hibernate.ogm.datastore.ehcache.impl.configuration.EhcacheConfiguration;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DefaultDatastoreNames;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;

/**
 * @author Alex Snaps
 */
public class EhcacheDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable,
		ServiceRegistryAwareService, Configurable {

	private JtaPlatform jtaPlatform;
	private CacheManager cacheManager;
	private Cache<SerializableEntityKey> entityCache;
	private Cache<SerializableAssociationKey> associationCache;
	private Cache<SerializableIdSourceKey> identifierCache;

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

		entityCache = new Cache<SerializableEntityKey>( cacheManager.getCache( DefaultDatastoreNames.ENTITY_STORE ) );
		associationCache = new Cache<SerializableAssociationKey>( cacheManager.getCache( DefaultDatastoreNames.ASSOCIATION_STORE ) );
		identifierCache = new Cache<SerializableIdSourceKey>( cacheManager.getCache( DefaultDatastoreNames.IDENTIFIER_STORE ) );
	}

	@Override
	public void stop() {
		cacheManager.shutdown();
	}

	public Cache<SerializableEntityKey> getEntityCache() {
		return entityCache;
	}

	public Cache<SerializableAssociationKey> getAssociationCache() {
		return associationCache;
	}

	public Cache<SerializableIdSourceKey> getIdentifierCache() {
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
