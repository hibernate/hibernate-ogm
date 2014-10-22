/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.model.impl;

import java.util.List;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;

/**
 * A {@link CouchDBAssociation} backed by an {@link EntityDocument}.
 *
 * @author Gunnar Morling
 */
class EmbeddedAssociation extends CouchDBAssociation {

	private final EntityDocument entity;
	private final String name;

	public EmbeddedAssociation(EntityDocument entity, String name) {
		this.entity = entity;
		this.name = name;
	}

	@Override
	public List<Object> getRows() {
		return entity.getAssociation( name );
	}

	@Override
	public void setRows(List<Object> rows) {
		entity.set( name, rows );
	}

	@Override
	public Document getOwningDocument() {
		return entity;
	}
}
