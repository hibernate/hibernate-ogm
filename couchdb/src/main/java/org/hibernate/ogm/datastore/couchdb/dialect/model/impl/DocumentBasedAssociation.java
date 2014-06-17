/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.model.impl;

import java.util.List;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.AssociationDocument;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;

/**
 * A {@link CouchDBAssociation} backed by an {@link AssociationDocument}.
 *
 * @author Gunnar Morling
 */
class DocumentBasedAssociation extends CouchDBAssociation {

	private final AssociationDocument document;

	public DocumentBasedAssociation(AssociationDocument document) {
		this.document = document;
	}

	@Override
	public List<Object> getRows() {
		return document.getRows();
	}

	@Override
	public void setRows(List<Object> rows) {
		document.setRows( rows );
	}

	@Override
	public Document getOwningDocument() {
		return document;
	}
}
