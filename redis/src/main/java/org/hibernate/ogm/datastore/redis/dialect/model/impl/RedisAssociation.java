/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.dialect.model.impl;

import java.util.Map;

import org.hibernate.ogm.datastore.redis.dialect.value.Association;
import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.dialect.value.StructuredValue;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;

/**
 * Represents an association stored in Redis, backed either by an association value (external storage of
 * associations) or an association sub-tree within an entity value (embedded storage of associations).
 * <p>
 * The owning document must be written back to Redis to make changes to the rows of an association persistent in the
 * data store.
 *
 * @author Gunnar Morling
 */
public abstract class RedisAssociation {

	/**
	 * Creates a {@link RedisAssociation} from the given {@link Entity} and association name.
	 *
	 * @param entity the owner of the association
	 * @param associationKeyMetadata association key meta-data
	 *
	 * @return a {@link RedisAssociation} representing the association
	 */
	public static RedisAssociation fromEmbeddedAssociation(
			Entity entity,
			AssociationKeyMetadata associationKeyMetadata) {
		return new EmbeddedAssociation( entity, associationKeyMetadata );
	}

	/**
	 * Creates a {@link RedisAssociation} from the given {@link java.util.Map} and association name.
	 *
	 * @param entity the owner of the association
	 * @param associationKeyMetadata association key meta-data
	 *
	 * @return a {@link RedisAssociation} representing the association
	 */
	public static RedisAssociation fromEmbeddedAssociation(
			Map<String, String> entity,
			AssociationKeyMetadata associationKeyMetadata) {
		return new HashEmbeddedAssociation( entity, associationKeyMetadata );
	}

	/**
	 * Creates a {@link RedisAssociation} from the given {@link Association}.
	 *
	 * @param associationDocument the document representing the association
	 *
	 * @return a {@link RedisAssociation} of the given {@link Association}
	 */
	public static RedisAssociation fromAssociationDocument(Association associationDocument) {
		return new DocumentBasedAssociation( associationDocument );
	}

	/**
	 * Get all the rows of this association. Does not contain columns which are part of the association
	 * key.
	 *
	 * @return a list with all the rows for this association
	 */
	public abstract Object getRows();

	/**
	 * Sets the rows of this association. The given list must not contain columns which are part of the association key.
	 *
	 * @param rows the rows of the association
	 */
	public abstract void setRows(Object rows);

	/**
	 * Returns the Redis value which owns this association, either an {@link Association} or an
	 * {@link Entity}.
	 *
	 * @return the {@link StructuredValue} representing the owner of the association
	 */
	public abstract StructuredValue getOwningDocument();
}
