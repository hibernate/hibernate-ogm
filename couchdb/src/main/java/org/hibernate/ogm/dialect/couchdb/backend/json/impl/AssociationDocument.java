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
package org.hibernate.ogm.dialect.couchdb.backend.json.impl;

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
