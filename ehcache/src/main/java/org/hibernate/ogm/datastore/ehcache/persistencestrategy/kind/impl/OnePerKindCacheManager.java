/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ehcache.persistencestrategy.kind.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hibernate.ogm.datastore.ehcache.impl.Cache;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.common.impl.CacheNames;
import org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl.LocalCacheManager;
import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TuplesSupplier;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;

import net.sf.ehcache.CacheManager;

/**
 * A {@link LocalCacheManager} which uses one cache for all entities, one cache for all associations and one cache for
 * all id sources.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public class OnePerKindCacheManager extends LocalCacheManager<SerializableEntityKey, SerializableAssociationKey, SerializableIdSourceKey> {

	private final Cache<SerializableEntityKey> entityCache;
	private final Cache<SerializableAssociationKey> associationCache;
	private final Cache<SerializableIdSourceKey> idSourceCache;

	public OnePerKindCacheManager(CacheManager cacheManager) {
		super( cacheManager );

		entityCache = new Cache<SerializableEntityKey>( cacheManager.getCache( CacheNames.ENTITY_CACHE ) );
		associationCache = new Cache<SerializableAssociationKey>( cacheManager.getCache( CacheNames.ASSOCIATION_CACHE ) );
		idSourceCache = new Cache<SerializableIdSourceKey>( cacheManager.getCache( CacheNames.IDENTIFIER_CACHE ) );
	}

	@Override
	public Cache<SerializableEntityKey> getEntityCache(EntityKeyMetadata keyMetadata) {
		return entityCache;
	}

	@Override
	public Cache<SerializableAssociationKey> getAssociationCache(AssociationKeyMetadata keyMetadata) {
		return associationCache;
	}

	@Override
	public Cache<SerializableIdSourceKey> getIdSourceCache(IdSourceKeyMetadata keyMetadata) {
		return idSourceCache;
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, EntityKeyMetadata... entityKeyMetadatas) {
		consumer.consume( new OnePerKindTuplesSupplier( entityCache, entityKeyMetadatas ) );
	}

	private static class OnePerKindTuplesSupplier implements TuplesSupplier {

		private final EntityKeyMetadata[] entityKeyMetadatas;
		private final Cache<SerializableEntityKey> entityCache;
		private final Iterator<SerializableEntityKey> keys;

		public OnePerKindTuplesSupplier(Cache<SerializableEntityKey> entityCache, EntityKeyMetadata... entityKeyMetadatas) {
			this.entityCache = entityCache;
			this.entityKeyMetadatas = entityKeyMetadatas;
			this.keys = entityCache.getKeys().iterator();
		}

		@Override
		public ClosableIterator<Tuple> get(TransactionContext transactionContext) {
			return new OnePerKindTupleIterator( keys, entityCache, entityKeyMetadatas );
		}
	}

	private static class OnePerKindTupleIterator implements ClosableIterator<Tuple> {

		private final Cache<SerializableEntityKey> entityCache;
		private final EntityKeyMetadata[] entityKeyMetadatas;
		private final Iterator<SerializableEntityKey> iterator;
		private SerializableEntityKey next;
		private boolean hasNext = false;

		public OnePerKindTupleIterator(Iterator<SerializableEntityKey> iterator, Cache<SerializableEntityKey> entityCache, EntityKeyMetadata[] entityKeyMetadatas2Iterator, EntityKeyMetadata... entityKeyMetadatas) {
			this.iterator = iterator;
			this.entityCache = entityCache;
			this.entityKeyMetadatas = entityKeyMetadatas;
			this.next = next( iterator );
		}

		private SerializableEntityKey next(Iterator<SerializableEntityKey> iterator) {
			SerializableEntityKey next = null;
			hasNext = false;
			while ( iterator.hasNext() ) {
				next = iterator.next();
				if ( isValidKey( next ) ) {
					hasNext = true;
					break;
				}
			}
			if ( hasNext ) {
				return next;
			}
			return null;
		}

		private boolean isValidKey(SerializableEntityKey key) {
			for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
				if ( entityKeyMetadata.getTable().equals( key.getTable() ) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public Tuple next() {
			if ( hasNext ) {
				Tuple current = createTuple( entityCache.get( this.next ) );
				this.next = next( this.iterator );
				return current;
			}

			throw new NoSuchElementException();
		}

		@Override
		public void close() {
		}
	}
}
