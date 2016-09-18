/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.json.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a query that can be executed on the remote server.
 * <p>
 * Results of the queries can be returned as graph elements (default) or as rows of a table.
 * More details in the Neo4j documentation:
 * http://neo4j.com/docs/2.3.4/rest-api-transactional.html#rest-api-return-results-in-graph-format
 *
 * @see Statement#AS_GRAPH
 * @see Statement#AS_ROW
 *
 * @author Davide D'Alto
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Statement {

	/**
	 * Value that can be used via set {@link Statement#setResultDataContents(List)} to obtain the results of the query
	 * as graph elements.
	 */
	public static final String AS_GRAPH = "graph";

	/**
	 * Value that can be used via set {@link Statement#setResultDataContents(List)} to obtain the results of the query
	 * as rows.
	 */
	public static final String AS_ROW = "row";

	private String statement;

	private Map<String, Object> parameters;

	private boolean includeStats = false;

	private List<String> resultDataContents = Arrays.asList( Statement.AS_GRAPH );

	public Statement() {
	}

	public Statement(String statement) {
		this.statement = statement;
	}

	public Statement(String statement, Map<String, Object> parameters) {
		this( statement );
		this.parameters = parameters;
	}

	public String getStatement() {
		return statement;
	}

	public void setStatement(String statement) {
		this.statement = statement;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public boolean isIncludeStats() {
		return includeStats;
	}

	public void setIncludeStats(boolean includeStats) {
		this.includeStats = includeStats;
	}

	public List<String> getResultDataContents() {
		return resultDataContents;
	}

	public void setResultDataContents(List<String> resultsDataContents) {
		this.resultDataContents = resultsDataContents;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( statement );
		return builder.toString();
	}
}
