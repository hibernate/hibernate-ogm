/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.ehcache.persistencestrategy.table.impl;

import org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.IdSourceKey;

/**
 * Provides the persistent keys for the "per-table" strategy. These keys don't contain the table name.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class PerTableKeyProvider implements KeyProvider<PerTableSerializableEntityKey, PerTableSerializableAssociationKey, PerTableSerializableIdSourceKey> {

	@Override
	public PerTableSerializableEntityKey getEntityCacheKey(EntityKey key) {
		return new PerTableSerializableEntityKey( key );
	}

	@Override
	public PerTableSerializableAssociationKey getAssociationCacheKey(AssociationKey key) {
		return new PerTableSerializableAssociationKey( key );
	}

	@Override
	public PerTableSerializableIdSourceKey getIdSourceCacheKey(IdSourceKey key) {
		return new PerTableSerializableIdSourceKey( key );
	}
}
