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
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.hibernate.ogm.dialect.couchdb.json.CouchDBAssociation;
import org.hibernate.ogm.dialect.couchdb.json.CouchDBDocument;

/**
 * Creates a CouchDB Design Document with a view and list used to retrieve the number of associations stored in the
 * database.
 * <p>
 * The map function of this view emits those documents whose type is {@link CouchDBAssociation#TYPE_NAME}. The reduce
 * function counts the number of the documents returned by the map function. The list function creates an easily
 * consumable representation of the view result.
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 * @author Gunnar Morling
 */
@JsonSerialize(include = Inclusion.NON_NULL)
public class AssociationsDesignDocument extends CouchDBDesignDocument {

	public static final String DOCUMENT_ID = "associations";
	public static final String VIEW_NAME = "allAssociations";
	public static final String LIST_NAME = "count";

	/**
	 * The URL to use in the REST call in order to obtain the number of associations stored in the database
	 */
	public static final String ASSOCIATION_COUNT_PATH = "_design/" + DOCUMENT_ID + "/_list/" + LIST_NAME + "/" + VIEW_NAME;

	/**
	 * The JavaScript map function; for each document of type "association" value 1 will be emitted.
	 */
	private static final String MAP = "function(doc) {if(doc." + CouchDBDocument.TYPE_DISCRIMINATOR_FIELD_NAME + " == \"" + CouchDBAssociation.TYPE_NAME
			+ "\"){  emit(doc.id, 1); }}";

	/**
	 * The JavaScript list function; simplifies the output of the view into the form { "count" : n }.
	 */
	private static final String LIST = "function (head, req) { row = getRow(); send( JSON.stringify( { count : row ? row.value : 0 } ) ); }";

	/**
	 * The JavaScript reduce function, return the length of the value returned by the map function, this value
	 * represents the number of the stored associations
	 */
	private static final String REDUCE = "_count";

	public AssociationsDesignDocument() {
		setId( DOCUMENT_ID );
		addView( VIEW_NAME, MAP, REDUCE );
		addList( LIST_NAME, LIST );
	}
}
