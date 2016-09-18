/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.json.impl;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * The result of a request via rest to a Neo4j server.
 *
 * @author Davide D'Alto
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatementsResponse {

	private List<StatementResult> results;

	private List<ErrorResponse> errors;

	public List<StatementResult> getResults() {
		return results;
	}

	public void setResults(List<StatementResult> results) {
		this.results = results;
	}

	public List<ErrorResponse> getErrors() {
		return errors;
	}

	public void setErrors(List<ErrorResponse> errors) {
		this.errors = errors;
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

		private static String toString(StatementsResponse response) {
			try {
				return writer.writeValueAsString( response );
			}
			catch (Exception e) {
				return response.getClass().getSimpleName() + ", error: " + e.getMessage();
			}
		}
	}
}
