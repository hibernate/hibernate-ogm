/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.ignite.IgniteDialect;
import org.hibernate.ogm.datastore.ignite.impl.IgniteDatastoreProvider;
import org.hibernate.ogm.datastore.ignite.impl.IgniteTupleSnapshot;
import org.hibernate.ogm.datastore.ignite.util.StringHelper;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.datastore.spi.DatastoreProvider;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.AssociationKind;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.TupleSnapshot;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.utils.GridDialectTestHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Dmitriy Kozlov
 */
public class IgniteTestHelper implements GridDialectTestHelper {

	@Override
	public long getNumberOfEntities(SessionFactory sessionFactory) {
		int entityCount = 0;
		Set<IgniteCache<?, ?>> processedCaches = Collections.newSetFromMap( new IdentityHashMap<IgniteCache<?, ?>, Boolean>() );
		for ( EntityPersister entityPersister : ( (SessionFactoryImplementor) sessionFactory ).getEntityPersisters().values() ) {
			IgniteCache<?, ?> entityCache = getEntityCache( sessionFactory, ( (OgmEntityPersister) entityPersister ).getEntityKeyMetadata() );
			if ( !processedCaches.contains( entityCache ) ) {
				entityCount += entityCache.size( CachePeekMode.ALL );
				processedCaches.add( entityCache );
			}
		}
		return entityCount;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory) {
		int associationCount = 0;
		IgniteDatastoreProvider datastoreProvider = getProvider( sessionFactory );
		for ( CollectionPersister collectionPersister : ( (SessionFactoryImplementor) sessionFactory ).getCollectionPersisters().values() ) {
			AssociationKeyMetadata associationKeyMetadata = ( (OgmCollectionPersister) collectionPersister ).getAssociationKeyMetadata();
			if ( associationKeyMetadata.getAssociationKind() == AssociationKind.ASSOCIATION ) {
				IgniteCache<Object, BinaryObject> associationCache = getAssociationCache( sessionFactory, associationKeyMetadata );
				StringBuilder query = new StringBuilder( "SELECT " )
											.append( StringHelper.realColumnName( associationKeyMetadata.getColumnNames()[0] ) )
											.append( " FROM " ).append( associationKeyMetadata.getTable() );
				SqlFieldsQuery sqlQuery = datastoreProvider.createSqlFieldsQueryWithLog( query.toString(), null );
				Iterable<List<?>> queryResult = associationCache.query( sqlQuery );
				Set<Object> uniqs = new HashSet<>();
				for ( List<?> row : queryResult ) {
					Object value = row.get( 0 );
					if ( value != null ) {
						uniqs.add( value );
					}
				}
				associationCount += uniqs.size();
			}
		}
		return associationCount;
	}

	@Override
	public long getNumberOfAssociations(SessionFactory sessionFactory, AssociationStorageType type) {
		int asscociationCount = 0;
		Set<IgniteCache<Object, BinaryObject>> processedCaches = Collections.newSetFromMap( new IdentityHashMap<IgniteCache<Object, BinaryObject>, Boolean>() );

		for ( CollectionPersister collectionPersister : ( (SessionFactoryImplementor) sessionFactory ).getCollectionPersisters().values() ) {
			AssociationKeyMetadata associationKeyMetadata = ( (OgmCollectionPersister) collectionPersister ).getAssociationKeyMetadata();
			IgniteCache<Object, BinaryObject> associationCache = getAssociationCache( sessionFactory, associationKeyMetadata );
			if ( !processedCaches.contains( associationCache ) ) {
				asscociationCount += associationCache.size();
				processedCaches.add( associationCache );
			}
		}

		return asscociationCount;
	}

	@Override
	public Map<String, Object> extractEntityTuple(Session session, EntityKey key) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) session.getSessionFactory();
		IgniteCache<Object, BinaryObject> cache = getEntityCache( sessionFactory, key.getMetadata() );
		Object cacheKey = getProvider( sessionFactory ).createKeyObject( key );

		Map<String, Object> result = new HashMap<>();
		BinaryObject po = cache.get( cacheKey );

		IgniteDialect igniteDialect = (IgniteDialect) sessionFactory.getServiceRegistry().getService( GridDialect.class );
		TupleSnapshot snapshot = new IgniteTupleSnapshot( cacheKey, po, key.getMetadata() );
		for ( String fieldName : snapshot.getColumnNames() ) {
			result.put( fieldName, snapshot.get( fieldName ) );
		}

		return result;
	}

	@Override
	public boolean backendSupportsTransactions() {
		return true;
	}

	@Override
	public void dropSchemaAndDatabase(SessionFactory sessionFactory) {
		if ( Ignition.allGrids().size() > 1 ) { // some tests doesn't stop DatastareProvider
			String currentGridName = getProvider( sessionFactory ).getGridName();
			for ( Ignite grid : Ignition.allGrids() ) {
				if ( !Objects.equals( currentGridName, grid.name() ) ) {
					grid.close();
				}
			}
		}
	}

	@Override
	public Map<String, String> getEnvironmentProperties() {
		return null;
	}

	@Override
	public GridDialect getGridDialect(DatastoreProvider datastoreProvider) {
		return new IgniteDialect( (IgniteDatastoreProvider) datastoreProvider );
	}

	public static IgniteCache<Object, BinaryObject> getEntityCache(SessionFactory sessionFactory, EntityKeyMetadata entityKeyMetadata) {
		IgniteDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getEntityCache( entityKeyMetadata );
	}

	public static IgniteCache<Object, BinaryObject> getAssociationCache(SessionFactory sessionFactory, AssociationKeyMetadata associationKeyMetadata) {
		IgniteDatastoreProvider castProvider = getProvider( sessionFactory );
		return castProvider.getAssociationCache( associationKeyMetadata );
	}

	private static IgniteDatastoreProvider getProvider(SessionFactory sessionFactory) {
		DatastoreProvider provider = ( (SessionFactoryImplementor) sessionFactory ).getServiceRegistry().getService( DatastoreProvider.class );
		if ( !( provider instanceof IgniteDatastoreProvider ) ) {
			throw new RuntimeException( "Not testing with Ignite, cannot extract underlying cache" );
		}
		return (IgniteDatastoreProvider) provider;
	}

	@Override
	public Class<? extends DatastoreConfiguration<?>> getDatastoreConfigurationType() {
		return org.hibernate.ogm.datastore.ignite.Ignite.class;
	}

	@Override
	public long getNumberOfEntities(Session session) {
		return getNumberOfEntities( session.getSessionFactory() );
	}

	@Override
	public long getNumberOfAssociations(Session session) {
		return getNumberOfAssociations( session.getSessionFactory() );
	}

}
