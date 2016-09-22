/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.model.spi;

import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.util.Experimental;

/**
 * Regroups the EntityKeyMetadata and the type name of the entity as it is useful to have them both when dealing
 * with querying.
 *
 * @author Guillaume Smet
 */
@Experimental
public class EntityMetadataInformation {

	private final EntityKeyMetadata entityKeyMetadata;
	private final String typeName;

	public EntityMetadataInformation(EntityKeyMetadata metadata, String typeName) {
		this.entityKeyMetadata = metadata;
		this.typeName = typeName;
	}

	public EntityKeyMetadata getEntityKeyMetadata() {
		return entityKeyMetadata;
	}

	public String getTypeName() {
		return typeName;
	}
}
