/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.dialect.model.impl;

import org.hibernate.ogm.datastore.document.association.spi.AssociationRows;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.spi.AssociationSnapshot;

/**
 * {@link AssociationSnapshot} implementation based on a {@link RedisAssociation} (which in turn wraps an association
 * value or an association stored within an entity value) as written to and retrieved from the Redis server.
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Gunnar Morling
 */
public class RedisAssociationSnapshot extends AssociationRows {

	/**
	 * The original association representing this snapshot as retrieved from Redis.
	 */
	private final RedisAssociation redisAssociation;

	public RedisAssociationSnapshot(RedisAssociation association, AssociationKey key) {
		super( key, association.getRows(), RedisAssociationRowFactory.INSTANCE );
		this.redisAssociation = association;
	}

	public RedisAssociation getRedisAssociation() {
		return redisAssociation;
	}
}
