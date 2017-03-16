/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.infinispan.persistencestrategy.kind.impl;

import java.util.Map;

import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
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
