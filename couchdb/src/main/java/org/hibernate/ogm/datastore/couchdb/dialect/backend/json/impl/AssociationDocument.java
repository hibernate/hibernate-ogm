/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion;

/**
 * Represents an association stored as a separate CouchDB document.
 *
 * Used to serialize and deserialize the JSON with the following structure:
 *
 * { "_id": "", "_rev": " ", "$type": "association", "rows": [{ "key": "value" }] }
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 * @author Gunnar Morling
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonTypeName(AssociationDocument.TYPE_NAME)
public class AssociationDocument extends Document {

	/**
	 * The name of this document type as materialized in {@link Document#TYPE_DISCRIMINATOR_FIELD_NAME}.
	 */
	public static final String TYPE_NAME = "association";

	private List<Object> rows = new ArrayList<Object>();

	public AssociationDocument() {
	}

	public AssociationDocument(String id) {
		super( id );
	}

	public List<Object> getRows() {
		return rows;
	}

	public void setRows(List<Object> rows) {
		this.rows = rows;
	}
}
