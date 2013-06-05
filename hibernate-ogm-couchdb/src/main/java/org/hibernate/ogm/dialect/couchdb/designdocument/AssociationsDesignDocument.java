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
import org.hibernate.ogm.dialect.couchdb.resteasy.CouchDBAssociation;

/**
 * Creates a CouchDb Design Document used to retrieve the number of {@link CouchDBAssociation}, stored in the database.
 *
 * CouchDBAssociation are stored in the Database with a JSON field 'type' = 'CouchDBAssociation'
 *
 * This field is used in the map function to extract only the documents related to CouchDBAssociation
 *
 * The reduce function counts the number of the documents returned by the map function
 *
 * @author Andrea Boriero <dreborier@gmail.com>
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class AssociationsDesignDocument extends CouchDBDesignDocument {

	/**
	 * The ID fo the Document
	 */
	public static final String DOCUMENT_ID = "_design/associations";
	/**
	 * The name of the view
	 */
	public static final String ASSOCIATIONS_NUMBER_VIEW_NAME = "number";
	/**
	 * The URL to use in the REST call in order to obtain the number number of CouchDBAssociation stored in
	 * the database
	 */
	public static final String NUMBER_OF_ASSOCIATIONS_VIEW_PATH = DOCUMENT_ID + "/_view/"
			+ ASSOCIATIONS_NUMBER_VIEW_NAME;

	/**
	 * The javascript used in the map function, for each stored document if the type is equal to
	 * the .class simpleName emit 1
	 */
	private static final String MAP = "function(doc) {if(doc.type == \"" + CouchDBAssociation.class.getSimpleName()
			+ "\"){ emit(null, 1); }}";
	/**
	 * The javascript used in the reduce function, return the length of the value returned by the map
	 * function, this value represents the number of the stored CouchDBAssociation
	 */
	private static final String REDUCE = "function(key,value){ return value.length; }";

	public AssociationsDesignDocument() {
		setId( DOCUMENT_ID );
		addView( ASSOCIATIONS_NUMBER_VIEW_NAME, MAP, REDUCE );
	}

}
