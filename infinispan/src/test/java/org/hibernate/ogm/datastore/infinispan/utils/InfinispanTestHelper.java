/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.utils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.infinispan.Infinispan;
import org.hibernate.ogm.datastore.infinispan.InfinispanDialect;
import org.hibernate.ogm.datastore.infinispan.impl.InfinispanDatastoreProvider;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.utils.TestableGridDialect;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.infinispan.Cache;

/**
 * @author Sanne Grinovero &lt;sanne@hibernate.org&gt; (C) 2011 Red Hat Inc.
 */
public class InfinispanTestHelper implements TestableGridDialect {

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		int entityCount = 0;
		Set<Cache<?, ?>> processedCaches = Collections.newSetFromMap( new IdentityHashMap<Cache<?, ?>, Boolean>() );

		for ( EntityPersister entityPersister : ( (SessionFactoryImplementor) sessionFactory ).getEntityPersisters().values() ) {
			Cache<?, ?> entityCache = getEntityCache( sessionFactory, ( (OgmEntityPersister) entityPersister ).getEntityKeyMetadata() );
			if ( !processedCaches.contains( entityCache ) ) {
				entityCount += entityCache.size();
				processedCaches.add( entityCache );
			}
		}

		return entityCount;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		int asscociationCount = 0;
		Set<Cache<?, ?>> processedCaches = Collections.newSetFromMap( new IdentityHashMap<Cache<?, ?>, Boolean>() );

		for ( CollectionPersister collectionPersister : ( (SessionFactoryImplementor) sessionFactory ).getCollectionPersisters().values() ) {
			Cache<?, ?> associationCache = getAssociationCache( sessionFactory, ( (OgmCollectionPersister) collectionPersister ).getAssociationKeyMetadata() );
			if ( !processedCaches.contains( associationCache ) ) {
				asscociationCount += associationCache.size();
				processedCaches.add( associationCache );
			}
		}

		return asscociationCount;
	}

	@Override
	public Map<String, Object> extractEntityTuple(SessionFactory sessionFactory, EntityKey key) {
		InfinispanDatastoreProvider provider = getProvider( sessionFactory );
		return getEntityCache( sessionFactory, key.getMetadata() ).get( provider.getKeyProvider().getEntityCacheKey( key ) );
	}

	private static Cache<?, Map<String, Object>> getEntityCache(SessionFactory sessionFactory, EntityKeyMetadata entityKeyMetadata) {
		InfinispanDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getCacheManager().getEntityCache( entityKeyMetadata );
	}

	public static InfinispanDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( !( InfinispanDatastoreProvider.class.isInstance( provider ) ) ) {
			throw new RuntimeException( "Not testing with Infinispan, cannot extract underlying cache" );
		}
		return InfinispanDatastoreProvider.class.cast( provider );
	}

	private static Cache<?, ?> getAssociationCache(SessionFactory sessionFactory, AssociationKeyMetadata associationKeyMetadata) {
		InfinispanDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getCacheManager().getAssociationCache( associationKeyMetadata );
	}

	@Override
	public boolean backendSupportsTransactions() {
		return true;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		// Nothing to do
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
		return configuration.configureOptionsFor( Infinispan.class );
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new InfinispanDialect( (InfinispanDatastoreProvider) datastoreProvider );
	}
}
