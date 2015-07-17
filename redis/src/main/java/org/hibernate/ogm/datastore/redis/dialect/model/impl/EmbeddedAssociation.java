/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.dialect.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.datastore.redis.dialect.value.Entity;
import org.hibernate.ogm.datastore.redis.dialect.value.StructuredValue;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;

/**
 * A {@link RedisAssociation} backed by an {@link Entity}.
 *
 * @author Gunnar Morling
 */
class EmbeddedAssociation extends RedisAssociation {

	private final Entity entity;
	private final AssociationKeyMetadata associationKeyMetadata;

	public EmbeddedAssociation(Entity entity, AssociationKeyMetadata associationKeyMetadata) {
		this.entity = entity;
		this.associationKeyMetadata = associationKeyMetadata;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Object> getRows() {
		List<Object> rows;
		Object fieldValue = entity.getProperties().get( associationKeyMetadata.getCollectionRole() );

		if ( fieldValue == null ) {
			rows = Collections.emptyList();
		}
		else if ( associationKeyMetadata.isOneToOne() ) {
			rows = new ArrayList<Object>( 1 );
			rows.add( fieldValue );
		}
		else {
			rows = (List<Object>) fieldValue;
		}

		return rows;
	}

	@Override
	public void setRows(List<Object> rows) {
		if ( rows.isEmpty() ) {
			entity.removeAssociation( associationKeyMetadata.getCollectionRole() );
		}
		else {
			Object value = associationKeyMetadata.isOneToOne() ? rows.iterator().next() : rows;
			entity.set( associationKeyMetadata.getCollectionRole(), value );
		}
	}

	@Override
	public StructuredValue getOwningDocument() {
		return entity;
	}
}
