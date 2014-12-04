/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.utils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.ehcache.Ehcache;
import org.hibernate.ogm.datastore.ehcache.EhcacheDialect;
import org.hibernate.ogm.datastore.ehcache.impl.Cache;
import org.hibernate.ogm.datastore.ehcache.impl.EhcacheDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.impl.DefaultIdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.utils.TestableGridDialect;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Alex Snaps
 */
public class EhcacheTestHelper implements TestableGridDialect {

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		int entityCount = 0;
		Set<Cache<?>> processedCaches = Collections.newSetFromMap( new IdentityHashMap<Cache<?>, Boolean>() );

		for ( EntityPersister entityPersister : ( (SessionFactoryImplementor) sessionFactory ).getEntityPersisters().values() ) {
			Cache<?> entityCache = getEntityCache( sessionFactory, ( (OgmEntityPersister) entityPersister ).getEntityKeyMetadata() );
			if ( !processedCaches.contains( entityCache ) ) {
				entityCount += entityCache.getSize();
				processedCaches.add( entityCache );
			}
		}

		return entityCount;
	}

	public static Cache<?> getEntityCache(SessionFactory sessionFactory, EntityKeyMetadata entityKeyMetadata) {
		EhcacheDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getCacheManager().getEntityCache( entityKeyMetadata );
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		int asscociationCount = 0;
		Set<Cache<?>> processedCaches = Collections.newSetFromMap( new IdentityHashMap<Cache<?>, Boolean>() );

		for ( CollectionPersister collectionPersister : ( (SessionFactoryImplementor) sessionFactory ).getCollectionPersisters().values() ) {
			Cache<?> associationCache = getAssociationCache( sessionFactory, ( (OgmCollectionPersister) collectionPersister ).getAssociationKeyMetadata() );
			if ( !processedCaches.contains( associationCache ) ) {
				asscociationCount += associationCache.getSize();
				processedCaches.add( associationCache );
			}
		}

		return asscociationCount;
	}

	public static Cache<?> getAssociationCache(SessionFactory sessionFactory, AssociationKeyMetadata associationKeyMetadata) {
		EhcacheDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getCacheManager().getAssociationCache( associationKeyMetadata );
	}

	public static Cache<?> getIdSourceCache(SessionFactory sessionFactory, String tableName) {
		EhcacheDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getCacheManager().getIdSourceCache( DefaultIdSourceKeyMetadata.forTable( tableName, "sequence_name", "next_val" ) );
	}

	@Override
	public Map<String,Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		Cache cache = getEntityCache( sessionFactory, key.getMetadata() );
		Object cacheKey = getProvider( sessionFactory ).getKeyProvider().getEntityCacheKey( key );

		return (Map<String, Object>) cache.get( cacheKey ).getObjectValue();
	}

	private static EhcacheDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry()
				.getService( DatastoreProvider.class );
		if ( !( EhcacheDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Ehcache, cannot extract underlying cache" );
		}
		return EhcacheDatastoreProvider.class.cast( provider );
	}

	/**
	 * TODO - EHCache _is_ transactional. Turn this on. We could turn on XA or Local.
	 * Local will be faster. We will pick this up from the cache config.
	 *
	 * @return
	 */
	@Override
	public boolean backendSupportsTransactions() {
		return false;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		//Nothing to do
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return null;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		throw new UnsupportedOperationException( "This datastore does not support different association storage strategies." );
	}

	@Override
	public GlobalContext<?, ?> configureDatastore(OgmConfiguration configuration) {
		return configuration.configureOptionsFor( Ehcache.class );
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new EhcacheDialect( (EhcacheDatastoreProvider) datastoreProvider );
	}
}
