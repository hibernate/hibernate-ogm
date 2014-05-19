/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.util.impl.LogicalPhysicalConverterHelper;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public final class EntityKeyBuilder {

	private EntityKeyBuilder() {
	}

	//static method because the builder pattern version was showing up during profiling
	public static EntityKey fromPersister(
			final OgmEntityPersister persister,
			final Serializable id,
			SessionImplementor session) {
		return fromData(
				persister.getEntityKeyMetadata(),
				persister.getGridIdentifierType(),
				id,
				session );
	}

	//static method because the builder pattern version was showing up during profiling
	public static EntityKey fromData(
			EntityKeyMetadata entityKeyMetadata,
			GridType identifierGridType,
			final Serializable id,
			SessionImplementor session) {
		Object[] values = LogicalPhysicalConverterHelper.getColumnsValuesFromObjectValue(
				id,
				identifierGridType,
				entityKeyMetadata.getColumnNames(),
				session
		);
		return new EntityKey( entityKeyMetadata, values );
	}

}
