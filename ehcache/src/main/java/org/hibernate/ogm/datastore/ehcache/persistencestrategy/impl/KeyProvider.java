/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl;

import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.IdSourceKey;

/**
 * Converts the OGM-internal keys into the cache keys.
 * <p>
 * Implementations must be thread-safe.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 *
 * @param <EK> the entity cache key type
 * @param <AK> the association cache key type
 * @param <ISK> the identity source cache key type
 */
public interface KeyProvider<EK,AK,ISK> {

	EK getEntityCacheKey(EntityKey key);

	AK getAssociationCacheKey(AssociationKey key);

	ISK getIdSourceCacheKey(IdSourceKey key);
}
