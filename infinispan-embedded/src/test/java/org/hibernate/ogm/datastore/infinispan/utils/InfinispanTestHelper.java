/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.utils;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.InfinispanEmbedded;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanEmbeddedDatastoreProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentAssociationKey;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentEntityKey;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.ogm.utils.BaseGridDialectTestHelper;
import org.hibernate.ogm.utils.FileHelper;
import org.hibernate.ogm.utils.GridDialectTestHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.atomic.FineGrainedAtomicMap;

/**
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public class InfinispanTestHelper extends BaseGridDialectTestHelper implements GridDialectTestHelper {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		int entityCount = 0;
		Set<Cache<?, ?>> processedCaches = Collections.newSetFromMap( new IdentityHashMap<Cache<?, ?>, Boolean>() );

		for ( EntityPersister entityPersister : ( (SessionFactoryImplementor) sessionFactory ).getMetamodel().entityPersisters().values() ) {
			Cache<?, ?> entityCache = getEntityCache( sessionFactory, ( (OgmEntityPersister) entityPersister ).getEntityKeyMetadata() );
			if ( !processedCaches.contains( entityCache ) ) {

				// the new implementation of FineGrainedAtomicMap creates entries also for every fields
				// so it is necessary to skip field entries on count
				entityCount += entityCache.getAdvancedCache().cacheEntrySet().stream()
					.filter( cacheEntry -> cacheEntry.getKey() instanceof PersistentEntityKey )
					.count();

				processedCaches.add( entityCache );
			}
		}

		return entityCount;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		int asscociationCount = 0;
		Set<Cache<?, ?>> processedCaches = Collections.newSetFromMap( new IdentityHashMap<Cache<?, ?>, Boolean>() );

		for ( CollectionPersister collectionPersister : ( (SessionFactoryImplementor) sessionFactory ).getMetamodel().collectionPersisters().values() ) {
			Cache<?, ?> associationCache = getAssociationCache( sessionFactory, ( (OgmCollectionPersister) collectionPersister ).getAssociationKeyMetadata() );
			if ( !processedCaches.contains( associationCache ) ) {

				// the new implementation of FineGrainedAtomicMap creates entries also for every association fields
				// so it is necessary to skip field entries on count
				asscociationCount += associationCache.getAdvancedCache().cacheEntrySet().stream()
					.filter( cacheEntry -> cacheEntry.getKey() instanceof PersistentAssociationKey )
					.count();

				processedCaches.add( associationCache );
			}
		}

		return asscociationCount;
	}

	@Override
	public Map<String, Object> extractEntityTuple(Session session, EntityKey key) {
		InfinispanEmbeddedDatastoreProvider provider = getProvider( session.getSessionFactory() );
		Cache<PersistentEntityKey, Map<String, Object>> entityCache = (Cache<PersistentEntityKey, Map<String, Object>>) getEntityCache( session.getSessionFactory(), key.getMetadata() );
		PersistentEntityKey entityCacheKey = (PersistentEntityKey) provider.getKeyProvider().getEntityCacheKey( key );

		// the new implementation of FineGrainedAtomicMap creates entries also for every fields
		// direct extraction (without the use of FineGrainedAtomicMap) return the physical cache status (1 entry for each tuple + 1 entry for each field)
		// the use of FineGrainedAtomicMap reassembles the tuple in the expected way
		// see method InfinispanDialect#createTuple
		FineGrainedAtomicMap<String, Object> fineGrainedAtomicMap = AtomicMapLookup.getFineGrainedAtomicMap( entityCache, entityCacheKey, false );
		Map<String, Object> tupleMap = fineGrainedAtomicMap.entrySet()
			.stream()
			.collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );

		return tupleMap;
	}

	private static Cache<?, Map<String, Object>> getEntityCache(SessionFactory sessionFactory, EntityKeyMetadata entityKeyMetadata) {
		InfinispanEmbeddedDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getCacheManager().getEntityCache( entityKeyMetadata );
	}

	public static InfinispanEmbeddedDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( !( InfinispanEmbeddedDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Infinispan, cannot extract underlying cache" );
		}
		return InfinispanEmbeddedDatastoreProvider.class.cast( provider );
	}

	private static Cache<?, ?> getAssociationCache(SessionFactory sessionFactory, AssociationKeyMetadata associationKeyMetadata) {
		InfinispanEmbeddedDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getCacheManager().getAssociationCache( associationKeyMetadata );
	}

	@Override
	public boolean backendSupportsTransactions() {
		return true;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		// remove any persistent clustered counter files
		File file = new File( System.getProperty( "project.build.directory" ) + File.separator + "counters" );
		boolean delete = FileHelper.delete( file );

		if ( log.isTraceEnabled() ) {
			log.trace( "Try to remove temporary files: " + file + " deleted: " + delete );
		}
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new InfinispanDialect( (InfinispanEmbeddedDatastoreProvider) datastoreProvider );
	}

	@Override
	public Class<? extends DatastoreConfiguration<?>> getDatastoreConfigurationType() {
		return InfinispanEmbedded.class;
	}
}
