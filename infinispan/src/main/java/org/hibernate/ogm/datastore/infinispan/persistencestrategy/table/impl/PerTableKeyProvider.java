/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.AssociationKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.EntityKeyMetadataExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.IdSourceKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.RowKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentEntityKey;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentEntityKeyExternalizer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;

/**
 * Initial strategy that uses three caches. One for entities, one for associations and one for the identity sources.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
//TODO Consider the move the InfinispanDatastoreProvider#getCache from the provider
public class PerTableKeyProvider implements KeyProvider<PersistentEntityKey, AssociationKey, IdSourceKey> {

	@Override
	public PersistentEntityKey getEntityCacheKey(EntityKey key) {
		return PersistentEntityKey.fromEntityKey( key );
	}

	@Override
	public AssociationKey getAssociationCacheKey(AssociationKey key) {
		return key;
	}

	@Override
	public IdSourceKey getIdSourceCacheKey(IdSourceKey key) {
		return key;
	}

	@Override
	public Mapper getMapper(EntityKeyMetadata... entityKeyMetadatas) {
		return new TupleMapper(entityKeyMetadatas);
	}

	@Override
	public Set<AdvancedExternalizer<?>> getExternalizers() {
		Set<AdvancedExternalizer<?>> externalizers = new HashSet<AdvancedExternalizer<?>>( 5 );

		externalizers.add( PersistentEntityKeyExternalizer.INSTANCE );
		externalizers.add( AssociationKeyExternalizer.INSTANCE );
		externalizers.add( RowKeyExternalizer.INSTANCE );
		externalizers.add( EntityKeyMetadataExternalizer.INSTANCE );
		externalizers.add( IdSourceKeyExternalizer.INSTANCE );

		return Collections.unmodifiableSet( externalizers );
	}

	private static class TupleMapper implements Mapper<PersistentEntityKey, Map<String, Object>, PersistentEntityKey, Map<String, Object>> {

		private final EntityKeyMetadata[] entityKeyMetadatas;

		public TupleMapper(EntityKeyMetadata... entityKeyMetadatas) {
			this.entityKeyMetadatas = entityKeyMetadatas;
		}

		@Override
		public void map(PersistentEntityKey key, Map<String, Object> value, Collector<PersistentEntityKey, Map<String, Object>> collector) {
			for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
				collector.emit( key, value );
			}
		}
	}
}
