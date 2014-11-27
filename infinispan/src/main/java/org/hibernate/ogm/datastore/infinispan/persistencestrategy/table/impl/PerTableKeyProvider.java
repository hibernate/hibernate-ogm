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

import org.hibernate.ogm.datastore.infinispan.persistencestrategy.common.externalizer.impl.RowKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentAssociationKey;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentAssociationKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentEntityKey;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentEntityKeyExternalizer;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentIdSourceKey;
import org.hibernate.ogm.datastore.infinispan.persistencestrategy.table.externalizer.impl.PersistentIdSourceKeyExternalizer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;

/**
 * Provides the persistent keys for the "per-table" strategy. These keys don't contain the table name.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class PerTableKeyProvider implements KeyProvider<PersistentEntityKey, PersistentAssociationKey, PersistentIdSourceKey> {

	@Override
	public PersistentEntityKey getEntityCacheKey(EntityKey key) {
		return PersistentEntityKey.fromEntityKey( key );
	}

	@Override
	public PersistentAssociationKey getAssociationCacheKey(AssociationKey key) {
		return PersistentAssociationKey.fromAssociationKey( key );
	}

	@Override
	public PersistentIdSourceKey getIdSourceCacheKey(IdSourceKey key) {
		return PersistentIdSourceKey.fromIdSourceKey( key );
	}

	@Override
	public TupleMapper getMapper(EntityKeyMetadata... entityKeyMetadatas) {
		return TupleMapper.INSTANCE;
	}

	@Override
	public Set<AdvancedExternalizer<?>> getExternalizers() {
		Set<AdvancedExternalizer<?>> externalizers = new HashSet<AdvancedExternalizer<?>>( 5 );

		externalizers.add( PersistentEntityKeyExternalizer.INSTANCE );
		externalizers.add( PersistentAssociationKeyExternalizer.INSTANCE );
		externalizers.add( RowKeyExternalizer.INSTANCE );
		externalizers.add( PersistentIdSourceKeyExternalizer.INSTANCE );

		return Collections.unmodifiableSet( externalizers );
	}

	private static class TupleMapper implements Mapper<PersistentEntityKey, Map<String, Object>, PersistentEntityKey, Map<String, Object>> {

		private static final TupleMapper INSTANCE = new TupleMapper();

		@Override
		public void map(PersistentEntityKey key, Map<String, Object> value, Collector<PersistentEntityKey, Map<String, Object>> collector) {
			collector.emit( key, value );
		}
	}
}
