/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.procedures;

import java.util.Collections;
import java.util.stream.Stream;

import org.hibernate.ogm.backendtck.storedprocedures.Car;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

public class CarStoredProcedures {
	@Procedure(value = Car.SIMPLE_VALUE_PROC, mode = Mode.WRITE)
	public Stream<SimpleValueProcedure> simpleValueProcedure(@Name("id") long id) {
		return Collections.singletonList( new SimpleValueProcedure( (int) id ) ).stream();
	}

	@Procedure(value = Car.RESULT_SET_PROC, mode = Mode.WRITE)
	public Stream<ResultSetProcedure> resultSetProcedure(@Name("id") long id, @Name("title") String title) {
		return Collections.singletonList( new ResultSetProcedure( (int) id, title ) ).stream();
	}

	public static class SimpleValueProcedure {
		public Number id;

		public SimpleValueProcedure() {
		}

		public SimpleValueProcedure(Integer id) {
			this.id = id;
		}
	}

	public static class ResultSetProcedure {
		public Number id;
		public String title;

		public ResultSetProcedure() {
		}

		public ResultSetProcedure(Integer id) {
			this.id = id;
		}

		public ResultSetProcedure(Integer id, String title) {
			this.id = id;
			this.title = title;
		}
	}
}
