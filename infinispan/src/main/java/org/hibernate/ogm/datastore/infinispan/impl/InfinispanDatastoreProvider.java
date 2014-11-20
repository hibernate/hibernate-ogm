/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.impl;

import java.util.Map;
import java.util.Set;

import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.impl.configuration.InfinispanConfiguration;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.PersistenceStrategy;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.Startable;
import org.hibernate.service.spi.Stoppable;
import org.infinispan.manager.EmbeddedCacheManager;

/**
 * Provides access to Infinispan's CacheManager; one CacheManager is needed for all caches,
 * it can be taken via JNDI or started by this ServiceProvider; in this case it will also
 * be stopped when no longer needed.
 *
 * @author Sanne Grinovero
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class InfinispanDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable,
													ServiceRegistryAwareService, Configurable {

	private static final Log log = LoggerFactory.make();

	private JtaPlatform jtaPlatform;
	private JndiService jndiService;
	private EmbeddedCacheManager externalCacheManager;
	private final InfinispanConfiguration config = new InfinispanConfiguration();

	private PersistenceStrategy<?, ?, ?> persistenceStrategy;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return InfinispanDialect.class;
	}

	@Override
	public void start() {
		try {
			String jndiProperty = config.getJndiName();
			if ( jndiProperty != null ) {
				log.tracef( "Retrieving Infinispan from JNDI at %1$s", jndiProperty );
				externalCacheManager = (EmbeddedCacheManager) jndiService.locate( jndiProperty );
			}
		}
		catch (RuntimeException e) {
			throw log.unableToInitializeInfinispan( e );
		}

		// clear resources
		this.jndiService = null;
	}

	/**
	 * Initializes the persistence strategy to be used when accessing the datastore. In particular, all the required
	 * caches will be configured and initialized.
	 *
	 * @param entityTypes meta-data of all the entity types registed with the current session factory
	 */
	public void initializePersistenceStrategy(Set<EntityKeyMetadata> entityTypes) {
		persistenceStrategy = PersistenceStrategy.getPerKindStrategy(
				externalCacheManager,
				config.getConfigurationUrl(),
				jtaPlatform,
				entityTypes
		);

		// clear resources
		this.externalCacheManager = null;
		this.jtaPlatform = null;
	}

	public LocalCacheManager<?, ?, ?> getCacheManager() {
		return persistenceStrategy.getCacheManager();
	}

	public CacheAndKeyProvider<?, ?, ?> getKeyProvider() {
		return persistenceStrategy.getKeyProvider();
	}

	@Override
	public void stop() {
		persistenceStrategy.getCacheManager().stop();
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
	public Class<? extends SchemaDefiner> getSchemaDefinerType() {
		return CacheInitializer.class;
	}
}
