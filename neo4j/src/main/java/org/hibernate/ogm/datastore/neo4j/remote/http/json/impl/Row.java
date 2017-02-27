/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.json.impl;

import java.util.List;
import java.util.Map;

/**
 * The result of a {@link Statement} expressed as rows or {@link Graph}.
 *
 * @see Statement#AS_GRAPH
 * @see Statement#AS_ROW
 *
 * @author Davide D'Alto
 */
public class Row {

	private List<Object> row;

	private Graph graph;

	private List<Map<String, Object>> meta;

	public Graph getGraph() {
		return graph;
	}

	public List<Object> getRow() {
		return row;
	}

	public List<Map<String, Object>> getMeta() {
		return meta;
	}

	public void setMeta(List<Map<String, Object>> meta) {
		this.meta = meta;
	}

	public void setRow(List<Object> row) {
		this.row = row;
	}

	public void setGraph(Graph graph) {
		this.graph = graph;
	}
}
