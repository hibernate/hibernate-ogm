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
package org.hibernate.ogm.dialect.couchdb.backend.json.designdocument;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.hibernate.ogm.dialect.couchdb.backend.json.AssociationDocument;
import org.hibernate.ogm.dialect.couchdb.backend.json.Document;
import org.hibernate.ogm.dialect.couchdb.backend.json.EntityDocument;

/**
 * Creates a CouchDB Design Document with a view and list used to retrieve the number of associations stored in the
 * database.
 * <p>
 * The map function of this view emits a record for each association. The reduce function counts the number of the
 * documents returned by the map function. The list function creates an easily consumable representation of the view
 * result.
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 * @author Gunnar Morling
 */
@JsonSerialize(include = Inclusion.NON_NULL)
public class AssociationsDesignDocument extends DesignDocument {

	public static final String DOCUMENT_ID = "associations";
	public static final String VIEW_NAME = "allAssociations";
	public static final String LIST_NAME = "count";

	/**
	 * The URL to use in the REST call in order to obtain the number of associations stored in the database
	 */
	public static final String ASSOCIATION_COUNT_PATH = "_design/" + DOCUMENT_ID + "/_list/" + LIST_NAME + "/" + VIEW_NAME;

	/**
	 * The JavaScript map function; for each association - embedded or in a dedicated document - value 1 will be emitted.
	 */
	private static final String MAP =
			"function(doc) {\n" +
			// association document
			"    if(doc." + Document.TYPE_DISCRIMINATOR_FIELD_NAME + " == \"" + AssociationDocument.TYPE_NAME + "\") {\n" +
			"        emit('association', 1);\n" +
			"    }\n" +
			// embedded association; each embedded array is considered as association; note that this also would match
			// embedded collections; ignoring this for now as there is no test which makes use of embedded collections
			// and associations
			"    else if(doc." + Document.TYPE_DISCRIMINATOR_FIELD_NAME + " == \"" + EntityDocument.TYPE_NAME + "\") {\n" +
			"        for(var propt in doc) {\n" +
			"            if( Object.prototype.toString.call( doc[propt] ) === '[object Array]' ) {\n" +
			"                emit('inEntity', 1 );\n" +
			"            }\n" +
			"        }\n" +
			"    }\n" +
			"}";

	/**
	 * The JavaScript reduce function, return the length of the value returned by the map function, this value
	 * represents the number of the stored associations
	 */
	private static final String REDUCE = "_count";

	/**
	 * The JavaScript list function; simplifies the output of the view into the form { "count" : n }.
	 */
	private static final String LIST =
			"function (head, req) {\n" +
			"    associationDocumentCount = 0;\n" +
			"    inEntityAssociationCount = 0;\n" +
			"    while ( row = getRow() ) {\n" +
			"        if ( row.key == \"association\" ) {\n" +
			"            associationDocumentCount = row.value;\n" +
			"        }\n" +
			"        else {\n" +
			"            inEntityAssociationCount = row.value;\n" +
			"        }\n" +
			"    }\n" +
			"    send(\n" +
			"        JSON.stringify( {\n" +
			"            associationDocumentCount : associationDocumentCount,\n" +
			"            inEntityAssociationCount : inEntityAssociationCount\n" +
			"        } )\n" +
			"    );\n" +
			"}";

	public AssociationsDesignDocument() {
		setId( DOCUMENT_ID );
		addView( VIEW_NAME, MAP, REDUCE );
		addList( LIST_NAME, LIST );
	}
}
