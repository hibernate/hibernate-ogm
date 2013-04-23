/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.dialect.couchdb.designdocument;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBEntity;

/**
 * Creates a CouchDB Design Document used to retrieve the {@link CouchDBEntity} whose tableName attribute is equal to
 * the value passed as a QueryParam in the REST call.
 *
 * CouchDBEntity are stored in the Database with a JSON field 'type' = 'CouchDBEntity' and a JSON field 'tableName' with
 * the name of the table the Entity belongs to.
 *
 * This field is used in the map function to extract only the documents related to CouchDBEntity with the tableName
 * field
 * equals to the value supplied as a QueryParam in the REST call (CouchDB use this value in the emit part of the map
 * function )
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class TuplesDesignDocument extends CouchDBDesignDocument {

	/**
	 * The ID fo the Document
	 */
	public static final String DOCUMENT_ID = "_design/tuples";
	/**
	 * The name of the view
	 */
	public static final String ENTITY_TUPLE_BY_TABLE_NAME_VIEW_NAME = "tuplesByTableName";
	/**
	 * The URL to use in the REST call in order to obtain the EntityTupleRows
	 */
	public static final String ENTITY_TUPLE_BY_TABLE_NAME_PATH = DOCUMENT_ID + "/_view/"
			+ ENTITY_TUPLE_BY_TABLE_NAME_VIEW_NAME;

	/**
	 * The javascript used in the map function, for each stored document if the type is equal to
	 * the CouchDBEntity.class and the doc.tableName is equal the value passed as a QueryParam in the REST call
	 * return the entire document
	 */
	public static final String MAP = "function(doc) {if(doc.type == \"" + CouchDBEntity.class.getSimpleName()
			+ "\") {emit(doc.tableName , doc);}}";

	public TuplesDesignDocument() {
		setId( DOCUMENT_ID );
		addView( ENTITY_TUPLE_BY_TABLE_NAME_VIEW_NAME, MAP );
	}

}
