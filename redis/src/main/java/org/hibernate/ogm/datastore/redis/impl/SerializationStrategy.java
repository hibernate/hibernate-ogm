/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.impl;

import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.type.Type;

/**
 * Strategy for serializing/deserializing entities within Redis.
 * <p>
 * Every strategy can support a different set of data types.
 *
 * @author Mark Paluch
 */
public interface SerializationStrategy {

	/**
	 * Deserialize payload into the expected {@code targetType}
	 *
	 * @param serialized string in the serialized form
	 * @param targetType expected type
	 * @param <T> expected type
	 *
	 * @return the instance of {@code targetType} or null
	 */
	<T> T deserialize(String serialized, Class<T> targetType);

	/**
	 * Serialize the {@code payload} into the target representation.
	 *
	 * @param payload the payload
	 *
	 * @return string containing the target representation
	 */
	String serialize(Object payload);

	/**
	 * If the datastore does not support a {@link Type} the dialect might override it with a custom one.
	 *
	 * @param type The {@link Type} that might need to be overridden
	 *
	 * @return the GridType instance to use to bind the given {@code type} or null if the type does not need to be overridden
	 */
	GridType overrideType(Type type);

}
