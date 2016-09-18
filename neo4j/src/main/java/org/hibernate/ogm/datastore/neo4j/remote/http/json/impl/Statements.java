/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.http.json.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The list of queries to execute via Rest.
 *
 * @author Davide D'Alto
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Statements {

	private List<Statement> statements = new ArrayList<Statement>();

	public List<Statement> getStatements() {
		return statements;
	}

	public void setStatements(List<Statement> statements) {
		this.statements = statements;
	}

	@JsonIgnore
	public void addStatement(String statement) {
		statements.add( new Statement( statement ) );
	}

	@JsonIgnore
	public void addStatement(Statement statement) {
		statements.add( statement );
	}

	@JsonIgnore
	public void addStatement(String query, Map<String, Object> params, String... dataContents) {
		Statement statement = new Statement( query, params );
		if ( dataContents != null && dataContents.length != 0 ) {
			statement.setResultDataContents( Arrays.asList( dataContents ) );
		}
		statements.add( statement );
	}
}
