/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.json.impl;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The result of a single query
 *
 * @author Davide D'Alto
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatementResult {

	private List<String> columns;

	private List<Row> data;

	public List<String> getColumns() {
		return columns;
	}

	public void setColumns(List<String> columns) {
		this.columns = columns;
	}

	public List<Row> getData() {
		return data;
	}

	public void setData(List<Row> data) {
		this.data = data;
	}
}
