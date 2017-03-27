/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdb.utils;

import java.util.List;

/**
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
public abstract class AbstractQueryGenerator {

	public static class GenerationResult {

		private List<Object> preparedStatementParams;
		private String executionQuery;

		public GenerationResult(List<Object> preparedStatementParams, String executionQuery) {
			this.preparedStatementParams = preparedStatementParams;
			this.executionQuery = executionQuery;
		}

		public List<Object> getPreparedStatementParams() {
			return preparedStatementParams;
		}

		public String getExecutionQuery() {
			return executionQuery;
		}
	}

}
