/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.common.request.impl;

import java.util.Collections;
import java.util.Map;

import org.hibernate.ogm.datastore.neo4j.remote.http.json.impl.Statement;

/**
 * Contains a query that can be executed remotely.
 *
 * @author Davide D'Alto
 */
public class RemoteStatement {

	private final String query;

	private final Map<String, Object> params;

	private final boolean asRow;

	public RemoteStatement(String queryString) {
		this( queryString, Collections.<String, Object>emptyMap() );
	}

	public RemoteStatement(String query, Map<String, Object> params) {
		this( query, params, false );
	}

	public RemoteStatement(String query, Map<String, Object> params, boolean asRow) {
		this.query = query;
		this.params = params;
		this.asRow = asRow;
	}

	public String getQuery() {
		return query;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	/**
	 * @see Statement#AS_ROW
	 */
	public boolean isAsRow() {
		return asRow;
	}
}
