/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

/**
 * Represents an association stored as a separate CouchDB document.
 *
 * Used to serialize and deserialize the JSON with the following structure:
 *
 * { "_id": "", "_rev": " ", "$type": "association", "rows": [{ "key": "value" }] }
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 * @author Gunnar Morling
 */
@JsonSerialize(include = Inclusion.NON_NULL)
@JsonTypeName(AssociationDocument.TYPE_NAME)
public class AssociationDocument extends Document {

	/**
	 * The name of this document type as materialized in {@link Document#TYPE_DISCRIMINATOR_FIELD_NAME}.
	 */
	public static final String TYPE_NAME = "association";

	private List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();

	public AssociationDocument() {
	}

	public AssociationDocument(String id) {
		super( id );
	}

	public List<Map<String, Object>> getRows() {
		return rows;
	}

	public void setRows(List<Map<String, Object>> rows) {
		this.rows = rows;
	}
}
