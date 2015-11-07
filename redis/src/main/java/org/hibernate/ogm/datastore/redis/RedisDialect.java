/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis;

import org.hibernate.ogm.datastore.redis.dialect.value.Association;
import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.impl.RedisDatastoreProvider;

/**
 * Stores tuples and associations inside Redis.
 * <p/>
 * Tuples are stored in Redis as a JSON serialization of a {@link Entity} object. Associations are stored in Redis obtained as a
 * JSON serialization of a {@link Association} object either within the entity or external.
 * See {@link org.hibernate.ogm.datastore.document.cfg.DocumentStoreProperties#ASSOCIATIONS_STORE} on how to configure
 * entity or external storage.
 *
 * @author Mark Paluch
 * @deprecated Use {@link RedisJsonDialect}
 */
@Deprecated
public class RedisDialect extends RedisJsonDialect {
	public RedisDialect(RedisDatastoreProvider provider) {
		super( provider );
	}
}
