/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.impl;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.transaction.TransactionManager;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.ehcache.EhcacheDialect;
import org.hibernate.ogm.datastore.ehcache.configuration.impl.EhcacheConfiguration;
import org.hibernate.ogm.datastore.ehcache.logging.impl.Log;
import org.hibernate.ogm.datastore.ehcache.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl.PersistenceStrategy;
import org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
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

	private static final Log logger = LoggerFactory.getLogger();

	private JtaPlatform jtaPlatform;
	private CacheManager cacheManager;

	private final EhcacheConfiguration config = new EhcacheConfiguration();

	private PersistenceStrategy<?, ?, ?> persistenceStrategy;

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
		try {
			final Configuration configuration = ConfigurationFactory.parseConfiguration( config.getUrl() );
			if ( jtaPlatform != null ) {
				OgmTransactionManagerLookupDelegate.transactionManager = jtaPlatform.retrieveTransactionManager();
				final FactoryConfiguration transactionManagerLookupParameter = new FactoryConfiguration();
				transactionManagerLookupParameter.setClass( OgmTransactionManagerLookupDelegate.class.getName() );
				configuration.addTransactionManagerLookup( transactionManagerLookupParameter );
			}
			cacheManager = CacheManager.create( config.getUrl() );
		}
		catch (Exception e) {
			// Wrap Exception in a ServiceException to make the stack trace more friendly
			// Otherwise a generic unable to request service is thrown
			throw logger.unableToStartDatastoreProvider( e );
		}
	}

	/**
	 * Initializes the persistence strategy to be used when accessing the datastore. In particular, all the required
	 * caches will be configured and initialized.
	 *
	 * @param cacheMappingType the {@link org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType} to be used
	 * @param entityTypes meta-data of all the entity types registered with the current session factory
	 * @param associationTypes meta-data of all the association types registered with the current session factory
	 * @param idSourceTypes meta-data of all the id source types registered with the current session factory
	 */
	public void initializePersistenceStrategy(CacheMappingType cacheMappingType, Set<EntityKeyMetadata> entityTypes, Set<AssociationKeyMetadata> associationTypes, Set<IdSourceKeyMetadata> idSourceTypes) {
		persistenceStrategy = PersistenceStrategy.getInstance(
				cacheMappingType,
				cacheManager,
				entityTypes,
				associationTypes,
				idSourceTypes
		);

		// clear resources
		this.cacheManager = null;
		this.jtaPlatform = null;
	}

	public LocalCacheManager<?, ?, ?> getCacheManager() {
		return persistenceStrategy.getCacheManager();
	}

	public KeyProvider<?, ?, ?> getKeyProvider() {
		return persistenceStrategy.getKeyProvider();
	}

	@Override
	public void stop() {
		if ( persistenceStrategy != null ) {
			persistenceStrategy.getCacheManager().stop();
		}
	}

	@Override
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return CacheInitializer.class;
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
