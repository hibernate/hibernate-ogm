/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.dialect.backend.json.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Used to serialize and deserialize a REST CouchDB response to a PUT and a DELETE
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericResponse {

	private boolean ok;
	private String rev;
	private String error;
	private String reason;
	private String id;

	public boolean getOk() {
		return ok;
	}

	public void setOk(boolean ok) {
		this.ok = ok;
	}

	public String getRev() {
		return rev;
	}

	public void setRev(String rev) {
		this.rev = rev;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
