/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Every Json document in CouchDB contains, The field '_id' representing the id of the document and '_rev' representing
 * the revision number of the document
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
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
