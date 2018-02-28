/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.storedprocedures;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.StoredProcedureParameter;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
@Entity
@NamedStoredProcedureQueries({
		@NamedStoredProcedureQuery(name = "testproc4_1", procedureName = Car.TEST_RESULT_SET_STORED_PROC,
			parameters = {
				@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = Void.class),
				@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
				@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class)
			}, resultClasses = Car.class),
		@NamedStoredProcedureQuery(name = "testproc4_2", procedureName = Car.TEST_RESULT_SET_STORED_PROC,
			parameters = {
				@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = Void.class),
				@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
				@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class)
				}, resultSetMappings = "carMapping"),
		@NamedStoredProcedureQuery(name = "testproc4_3", procedureName = Car.TEST_RESULT_SET_STORED_PROC,
			parameters = {
				@StoredProcedureParameter(name = "result", mode = ParameterMode.REF_CURSOR, type = Void.class),
				@StoredProcedureParameter(name = Car.TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME, mode = ParameterMode.IN, type = Integer.class),
				@StoredProcedureParameter(name = Car.TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME, mode = ParameterMode.IN, type = String.class)
			}, resultClasses = Car.class),
		@NamedStoredProcedureQuery(name = "testproc4_4", procedureName = Car.TEST_RESULT_SET_STORED_PROC,
			parameters = {
				@StoredProcedureParameter(name = "result", mode = ParameterMode.REF_CURSOR, type = Void.class),
				@StoredProcedureParameter(name = Car.TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME, mode = ParameterMode.IN, type = Integer.class),
				@StoredProcedureParameter(name = Car.TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME, mode = ParameterMode.IN, type = String.class)
			}, resultSetMappings = "carMapping")
})

@SqlResultSetMapping(name = "carMapping", entities = { @EntityResult(entityClass = Car.class) })
public class Car {

	public static final String TEST_SIMPLE_VALUE_STORED_PROC = "testSimpleValue";
	public static final String TEST_SIMPLE_VALUE_STORED_PROC_PARAM_NAME = "testSimpleValue";

	public static final String TEST_RESULT_SET_STORED_PROC = "testResultSet";
	public static final String TEST_RESULT_SET_STORED_PROC_ID_PARAM_NAME = "id";
	public static final String TEST_RESULT_SET_STORED_PROC_TITLE_PARAM_NAME = "title";

	@Id
	private Integer id;

	private String title;

	public Car() {
	}

	public Car(Integer id, String title) {
		this.id = id;
		this.title = title;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Car car = (Car) o;
		return Objects.equals( id, car.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}

	@Override
	public String toString() {
		return "Car{" + id + ", '" + title + '}';
	}
}
