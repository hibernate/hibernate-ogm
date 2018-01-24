/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.impl;

import static org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.ExternalizersIntegration.validateExternalizersPresent;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.configuration.impl.InfinispanConfiguration;
import org.hibernate.ogm.datastore.infinispan.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispan.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.counter.ClusteredCounterCommand;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.PersistenceStrategy;
import org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType;
import org.hibernate.ogm.datastore.spi.BaseDatastoreProvider;
import org.hibernate.ogm.datastore.spi.SchemaDefiner;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata.IdSourceType;
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
public class InfinispanEmbeddedDatastoreProvider extends BaseDatastoreProvider implements Startable, Stoppable,
													ServiceRegistryAwareService, Configurable {

	private static final Log LOG = LoggerFactory.make( MethodHandles.lookup() );

	private JtaPlatform jtaPlatform;
	private JndiService jndiService;
	private EmbeddedCacheManager externalCacheManager;
	private final InfinispanConfiguration config = new InfinispanConfiguration();

	private PersistenceStrategy<?, ?, ?> persistenceStrategy;

	private ClusteredCounterCommand counter;

	@Override
	public Class<? extends GridDialect> getDefaultDialect() {
		return InfinispanDialect.class;
	}

	@Override
	public void start() {
		try {
			String jndiProperty = config.getJndiName();
			if ( jndiProperty != null ) {
				LOG.tracef( "Retrieving Infinispan from JNDI at %1$s", jndiProperty );
				externalCacheManager = (EmbeddedCacheManager) jndiService.locate( jndiProperty );
			}
		}
		catch (RuntimeException e) {
			// return a ServiceException to be stack trace friendly
			throw LOG.unableToInitializeInfinispan( e );
		}
		if ( externalCacheManager != null ) {
			validateExternalizersPresent( externalCacheManager );
		}

		// clear resources
		this.jndiService = null;
	}

	/**
	 * Initializes the persistence strategy to be used when accessing the datastore. In particular, all the required
	 * caches will be configured and initialized.
	 *
	 * @param cacheMappingType the {@link org.hibernate.ogm.datastore.keyvalue.options.CacheMappingType} to be used
	 * @param entityTypes meta-data of all the entity types registered with the current session factory
	 * @param associationTypes meta-data of all the association types registered with the current session factory
	 * @param idSourceTypes meta-data of all the id source types registered with the current session factory
	 * @param namespaces from the database currently in use
	 */
	public void initializePersistenceStrategy(CacheMappingType cacheMappingType, Set<EntityKeyMetadata> entityTypes, Set<AssociationKeyMetadata> associationTypes, Set<IdSourceKeyMetadata> idSourceTypes, Iterable<Namespace> namespaces) {
		persistenceStrategy = PersistenceStrategy.getInstance(
				cacheMappingType,
				externalCacheManager,
				config.getConfigurationUrl(),
				jtaPlatform,
				entityTypes,
				associationTypes,
				idSourceTypes
		);

		boolean requiresCounter = hasSequences( namespaces ) || hasIdGeneration( idSourceTypes );
		if ( requiresCounter ) {
			this.counter = new ClusteredCounterCommand( persistenceStrategy.getCacheManager().getCacheManager() );
		}

		// clear resources
		this.externalCacheManager = null;
		this.jtaPlatform = null;
	}

	private boolean hasIdGeneration(Set<IdSourceKeyMetadata> idSourceKeys) {
		for ( IdSourceKeyMetadata idSourceKeyMetadata : idSourceKeys ) {
			if ( idSourceKeyMetadata.getType() == IdSourceType.TABLE ) {
				return true;
			}
		}
		return false;
	}

	private boolean hasSequences(Iterable<Namespace> namespaces) {
		for ( Namespace namespace : namespaces ) {
			if ( namespace.getSequences().iterator().hasNext() ) {
				return true;
			}
		}
		return false;
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

	public ClusteredCounterCommand getCounter() {
		return counter;
	}
}
