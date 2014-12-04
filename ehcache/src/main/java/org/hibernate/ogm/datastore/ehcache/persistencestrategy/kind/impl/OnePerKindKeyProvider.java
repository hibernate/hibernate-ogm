/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.ehcache.persistencestrategy.kind.impl;

import org.hibernate.ogm.datastore.ehcache.persistencestrategy.impl.KeyProvider;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.IdSourceKey;

/**
 * Key provider which stores keys by serializing all their attributes into Ehcache.
 *
 * @author Gunnar Morling
 */
public class OnePerKindKeyProvider implements KeyProvider<SerializableEntityKey, SerializableAssociationKey, SerializableIdSourceKey> {

	@Override
	public SerializableEntityKey getEntityCacheKey(EntityKey key) {
		return new SerializableEntityKey( key );
	}

	@Override
	public SerializableAssociationKey getAssociationCacheKey(AssociationKey key) {
		return new SerializableAssociationKey( key );
	}

	@Override
	public SerializableIdSourceKey getIdSourceCacheKey(IdSourceKey key) {
		return new SerializableIdSourceKey( key );
	}
}
