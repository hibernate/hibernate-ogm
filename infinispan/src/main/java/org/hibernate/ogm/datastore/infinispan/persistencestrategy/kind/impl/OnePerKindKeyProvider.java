/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl.RowKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.AssociationKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.EntityKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.EntityKeyMetadataExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.externalizer.impl.IdSourceKeyExternalizer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;

/**
 * Key provider which stores all keys as is in ISPN.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OnePerKindKeyProvider implements KeyProvider<EntityKey, AssociationKey, IdSourceKey> {

	@Override
	public EntityKey getEntityCacheKey(EntityKey key) {
		return key;
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
	public TupleMapper getMapper(EntityKeyMetadata... entityKeyMetadatas) {
		return new TupleMapper( entityKeyMetadatas );
	}

	@Override
	public Set<AdvancedExternalizer<?>> getExternalizers() {
		Set<AdvancedExternalizer<?>> externalizers = new HashSet<AdvancedExternalizer<?>>( 5 );

		externalizers.add( EntityKeyExternalizer.INSTANCE );
		externalizers.add( AssociationKeyExternalizer.INSTANCE );
		externalizers.add( RowKeyExternalizer.INSTANCE );
		externalizers.add( EntityKeyMetadataExternalizer.INSTANCE );
		externalizers.add( IdSourceKeyExternalizer.INSTANCE );

		return Collections.unmodifiableSet( externalizers );
	}

	private static class TupleMapper implements Mapper<EntityKey, Map<String, Object>, EntityKey, Map<String, Object>> {

		private final EntityKeyMetadata[] entityKeyMetadatas;

		public TupleMapper(EntityKeyMetadata... entityKeyMetadatas) {
			this.entityKeyMetadatas = entityKeyMetadatas;
		}

		@Override
		public void map(EntityKey key, Map<String, Object> value, Collector<EntityKey, Map<String, Object>> collector) {
			for ( EntityKeyMetadata entityKeyMetadata : entityKeyMetadatas ) {
				if ( key.getTable().equals( entityKeyMetadata.getTable() ) ) {
					collector.emit( key, value );
				}
			}
		}
	}
}
