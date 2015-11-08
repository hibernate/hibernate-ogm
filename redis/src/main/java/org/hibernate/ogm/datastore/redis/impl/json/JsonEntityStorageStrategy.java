/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl.json;

import java.util.Iterator;
import java.util.Set;

import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.model.spi.TupleOperation;

import com.lambdaworks.redis.RedisConnection;

/**
 * Map entities to top-level keys in Redis using {@literal GET} and {@literal SET} commands.
 * The value represents the entity as JSON. This strategy supports embedded associations and nested objects.
 *
 * @author Mark Paluch
 */
public class JsonEntityStorageStrategy  {

	private final JsonSerializationStrategy jsonSerializationStrategy;
	private final RedisConnection<String, String> connection;

	public JsonEntityStorageStrategy(
			JsonSerializationStrategy jsonSerializationStrategy,
			RedisConnection<String, String> connection) {
		this.jsonSerializationStrategy = jsonSerializationStrategy;
		this.connection = connection;
	}

	public Entity getEntity(String key) {
		String value = connection.get( key );
		return jsonSerializationStrategy.deserialize( value, Entity.class );
	}

	public void storeEntity(String key, Entity entity, Set<TupleOperation> operations) {
		String value = jsonSerializationStrategy.serialize( entity );

		connection.set( key, value );
	}

	public Iterable<Entity> getEntities(String[] keys) {
		final Iterator<String> values = connection.mget( keys ).iterator();

		return new Iterable<Entity>() {

			@Override
			public Iterator<Entity> iterator() {
				return new Iterator<Entity>() {

					@Override
					public boolean hasNext() {
						return values.hasNext();
					}

					@Override
					public Entity next() {
						String value = values.next();
						return value != null ? jsonSerializationStrategy.deserialize( value, Entity.class ) : null;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException( "Removal is not supported" );
					}
				};
			}
		};
	}
}
