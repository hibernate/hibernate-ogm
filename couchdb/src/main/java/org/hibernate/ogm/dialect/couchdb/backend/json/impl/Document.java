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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

/**
 * Every Json document in CouchDB contains, The field '_id' representing the id of the document and '_rev' representing
 * the revision number of the document
 *
 * @author Andrea Boriero <dreborier@gmail.com/>
 */
@JsonTypeInfo(
	use = JsonTypeInfo.Id.NAME,
	include = JsonTypeInfo.As.PROPERTY,
	property = Document.TYPE_DISCRIMINATOR_FIELD_NAME
)
public abstract class Document {

	/**
	 * Name of the document type discriminator field
	 */
	public static final String TYPE_DISCRIMINATOR_FIELD_NAME = "$type";

	public static final String REVISION_FIELD_NAME = "_rev";

	@JsonProperty("_id")
	private String id;

	@JsonProperty(REVISION_FIELD_NAME)
	private String revision;

	public Document() {
	}

	public Document(String id) {
		this.id = id;
	}

	public Document(String id, String revision) {
		this.id = id;
		this.revision = revision;
	}

	@JsonIgnore
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@JsonIgnore
	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	@Override
	public String toString() {
		return JsonToStringHelper.toString( this );
	}

	/**
	 * Creates a JSON representation of given documents. As static inner class this is only loaded on demand, i.e. when
	 * {@code toString()} is invoked on a document type.
	 *
	 * @author Gunnar Morling
	 */
	private static class JsonToStringHelper {

		/**
		 * Thread-safe as per the docs.
		 */
		private static final ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();

		private static String toString(Document document) {
			try {
				return writer.writeValueAsString( document );
			}
			catch (Exception e) {
				return document.getClass().getSimpleName() + " id: " + document.getId() + " rev: " + document.getRevision();
			}
		}
	}
}
