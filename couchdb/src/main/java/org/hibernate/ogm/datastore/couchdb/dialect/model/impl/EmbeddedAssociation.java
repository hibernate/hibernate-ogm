/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.model.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;
import org.hibernate.ogm.datastore.document.impl.DotPatternMapHelpers;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;

/**
 * A {@link CouchDBAssociation} backed by an {@link EntityDocument}.
 *
 * @author Gunnar Morling
 */
class EmbeddedAssociation extends CouchDBAssociation {

	private final EntityDocument entity;
	private final AssociationKeyMetadata associationKeyMetadata;

	public EmbeddedAssociation(EntityDocument entity, AssociationKeyMetadata associationKeyMetadata) {
		this.entity = entity;
		this.associationKeyMetadata = associationKeyMetadata;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object getRows() {
		Object rows;
		Object fieldValue = DotPatternMapHelpers.getValueOrNull(
				entity.getPropertiesAsHierarchy(), associationKeyMetadata.getCollectionRole()
		);

		if ( fieldValue == null ) {
			rows = Collections.emptyList();
		}
		else if ( associationKeyMetadata.isOneToOne() ) {
			rows = fieldValue;
		}
		else {
			rows = fieldValue;
		}

		return rows;
	}

	@Override
	public void setRows(Object rows) {
		if ( isEmpty( rows ) ) {
			entity.removeAssociation( associationKeyMetadata.getCollectionRole() );
		}
		else {

			entity.removeAssociation( associationKeyMetadata.getCollectionRole() );
			if ( associationKeyMetadata.isOneToOne() && rows instanceof Collection ) {
				Object value = ( (Collection) rows ).iterator().next();
				entity.set( associationKeyMetadata.getCollectionRole(), value );
			}
			else {
				entity.set( associationKeyMetadata.getCollectionRole(), rows );
			}
		}
	}

	protected boolean isEmpty(Object rows) {

		if ( rows == null ) {
			return true;
		}

		if ( rows instanceof Collection<?> && ( (Collection<?>) rows ).isEmpty() ) {
			return true;
		}

		if ( rows instanceof Map<?, ?> && ( (Map<?, ?>) rows ).isEmpty() ) {
			return true;
		}

		return false;
	}

	@Override
	public Document getOwningDocument() {
		return entity;
	}
}
