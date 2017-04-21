/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.dto;

/**
 * The class is presentation of generation of query
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public class GenerationResult {

	/**
	 * The query
	 */
	private String executionQuery;

	/**
	 * Contractor
	 *
	 * @param executionQuery string presentation of query
	 */
	public GenerationResult(String executionQuery) {
		this.executionQuery = executionQuery;
	}

	public String getExecutionQuery() {
		return executionQuery;
	}

}
