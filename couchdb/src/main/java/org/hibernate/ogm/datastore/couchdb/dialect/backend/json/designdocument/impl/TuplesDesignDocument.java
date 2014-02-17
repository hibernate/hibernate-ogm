/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.json.designdocument.impl;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.Document;
import org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl.EntityDocument;

/**
 * Creates a CouchDB Design Document with a view used to retrieve the entities of a given table.
 * <p>
 * The map function of this view emits those documents whose type is {@link EntityDocument#TYPE_NAME}, keyed by table
 * name. This allows to limit the result set to entities of specific tables by specifying the "key" query parameter when
 * querying the view.
 *
 * @author Andrea Boriero <dreborier@gmail.com>
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
