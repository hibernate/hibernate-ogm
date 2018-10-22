/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.embedded.dialect;

import org.neo4j.graphdb.ExecutionPlanDescription;
import org.neo4j.graphdb.Notification;
import org.neo4j.graphdb.QueryExecutionType;
import org.neo4j.graphdb.QueryStatistics;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DummyExecutionResult implements Result {
	private final List<String> columnNames;
	private final Iterator<Map<String, Object>> iterator;

	public DummyExecutionResult(List<String> columnNames, Collection<Map<String, Object>> rows) {
		this.columnNames = columnNames;
		this.iterator = rows.iterator();
	}

	@Override
	public QueryExecutionType getQueryExecutionType() {
		//not needed in the test
		return null;
	}

	@Override
	public List<String> columns() {
		return this.columnNames;
	}

	@Override
	public <T> ResourceIterator<T> columnAs(String name) {
		//not needed in the test
		return null;
	}

	@Override
	public boolean hasNext() {
		return this.iterator.hasNext();
	}

	@Override
	public Map<String, Object> next() {
		return this.iterator.next();
	}

	@Override
	public void close() {
		//not needed in the test
	}

	@Override
	public QueryStatistics getQueryStatistics() {
		//not needed in the test
		return null;
	}

	@Override
	public ExecutionPlanDescription getExecutionPlanDescription() {
		//not needed in the test
		return null;
	}

	@Override
	public String resultAsString() {
		//not needed in the test
		return null;
	}

	@Override
	public void writeAsStringTo(PrintWriter writer) {
		//not needed
	}

	@Override
	public void remove() {
		//not needed in the test
	}

	@Override
	public Iterable<Notification> getNotifications() {
		//not needed in the test
		return null;
	}

	@Override
	public <VisitationException extends Exception> void accept(ResultVisitor<VisitationException> visitor)
			throws VisitationException {
		//not needed in the test
	}
}
