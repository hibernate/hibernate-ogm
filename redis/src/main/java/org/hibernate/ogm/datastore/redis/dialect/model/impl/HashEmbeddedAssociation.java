/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.dialect.model.impl;

import java.util.Collection;
import java.util.Map;

import org.hibernate.ogm.datastore.redis.dialect.value.HashEntity;
import org.hibernate.ogm.datastore.redis.dialect.value.StructuredValue;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;

/**
 * A {@link RedisAssociation} backed by a Redis Hash.
 *
 * @author Mark Paluch
 */
class HashEmbeddedAssociation extends RedisAssociation {

	private final Map<String, String> entity;
	private final HashEntity hashEntity;
	private final AssociationKeyMetadata associationKeyMetadata;

	public HashEmbeddedAssociation(Map<String, String> entity, AssociationKeyMetadata associationKeyMetadata) {
		this.entity = entity;
		this.associationKeyMetadata = associationKeyMetadata;
		hashEntity = new HashEntity( entity );
	}

	@Override
	public Object getRows() {
		return entity.get( associationKeyMetadata.getCollectionRole() );
	}

	@Override
	public void setRows(Object rows) {

		if ( isEmpty( rows ) ) {
			entity.put( associationKeyMetadata.getCollectionRole(), null );
		}
		else {
			Object value = ( (Collection) rows ).iterator().next();
			entity.put( associationKeyMetadata.getCollectionRole(), (String) value );
		}
	}

	protected boolean isEmpty(Object rows) {

		if ( rows == null ) {
			return true;
		}

		if ( rows instanceof Collection<?> && ( (Collection<?>) rows ).isEmpty() ) {
			return true;
		}

		return false;
	}

	@Override
	public StructuredValue getOwningDocument() {
		return hashEntity;
	}
}
