/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.json.designdocument.impl;

import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Creates a CouchDB Design Document with a view used to retrieve the entities of a given table.
 * <p>
 * The map function of this view emits those documents whose type is {@link EntityDocument#TYPE_NAME}, keyed by table
 * name. This allows to limit the result set to entities of specific tables by specifying the "key" query parameter when
 * querying the view.
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class TuplesDesignDocument extends DesignDocument {

	/**
	 * The ID of the Document
	 */
	public static final String DOCUMENT_ID = "tuples";

	/**
	 * The name of the view
	 */
	public static final String ENTITY_TUPLE_BY_TABLE_NAME_VIEW_NAME = "tuplesByTableName";

	/**
	 * The URL to use in the REST call in order to obtain the EntityTupleRows
	 */
	public static final String ENTITY_TUPLE_BY_TABLE_NAME_PATH = "_design/" + DOCUMENT_ID + "/_view/"
			+ ENTITY_TUPLE_BY_TABLE_NAME_VIEW_NAME;

	/**
	 * The JavaScript map function; each document of type "entity" will be emitted, using the table name as key.
	 */
	public static final String MAP = "function(doc) {if(doc." + Document.TYPE_DISCRIMINATOR_FIELD_NAME + " == \"" + EntityDocument.TYPE_NAME
			+ "\") {emit(doc.$table , doc);}}";

	public TuplesDesignDocument() {
		setId( DOCUMENT_ID );
		addView( ENTITY_TUPLE_BY_TABLE_NAME_VIEW_NAME, MAP );
	}

}
