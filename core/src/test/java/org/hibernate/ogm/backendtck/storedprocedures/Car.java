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
 * This entity map a stored procedure.
 * <p>
 * The implementation of the store procedure can change between dialect but the idea
 * is that it will returned a {@link Car} initialized with the value passed as parameters.
 *
 * @author Davide D'Alto
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
@Entity
@NamedStoredProcedureQueries({
		@NamedStoredProcedureQuery(name = "returnPositionalParametersWithEntity", procedureName = Car.RESULT_SET_PROC,
			parameters = {
				@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = Void.class),
				@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
				@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class)
			}, resultClasses = Car.class),
		@NamedStoredProcedureQuery(name = "returnPositionalParametersWithMapping", procedureName = Car.RESULT_SET_PROC,
			parameters = {
				@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = Void.class),
				@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
				@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class)
				}, resultSetMappings = "carMapping"),
		@NamedStoredProcedureQuery(name = "returnPositionalParametersRaw", procedureName = Car.RESULT_SET_PROC,
			parameters = {
				@StoredProcedureParameter(mode = ParameterMode.REF_CURSOR, type = Void.class),
				@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
				@StoredProcedureParameter(mode = ParameterMode.IN, type = String.class)
				}),
		@NamedStoredProcedureQuery(name = "returnNamedParametersWithEntity", procedureName = Car.RESULT_SET_PROC,
			parameters = {
				@StoredProcedureParameter(name = "result", mode = ParameterMode.REF_CURSOR, type = Void.class),
				@StoredProcedureParameter(name = Car.RESULT_SET_PROC_ID_PARAM, mode = ParameterMode.IN, type = Integer.class),
				@StoredProcedureParameter(name = Car.RESULT_SET_PROC_TITLE_PARAM, mode = ParameterMode.IN, type = String.class)
			}, resultClasses = Car.class),
		@NamedStoredProcedureQuery(name = "returnNamedParametersWithMapping", procedureName = Car.RESULT_SET_PROC,
			parameters = {
				@StoredProcedureParameter(name = "result", mode = ParameterMode.REF_CURSOR, type = Void.class),
				@StoredProcedureParameter(name = Car.RESULT_SET_PROC_ID_PARAM, mode = ParameterMode.IN, type = Integer.class),
				@StoredProcedureParameter(name = Car.RESULT_SET_PROC_TITLE_PARAM, mode = ParameterMode.IN, type = String.class)
			}, resultSetMappings = "carMapping"),
		@NamedStoredProcedureQuery(name = "returnNamedParametersRaw", procedureName = Car.RESULT_SET_PROC,
			parameters = {
				@StoredProcedureParameter(name = "result", mode = ParameterMode.REF_CURSOR, type = Void.class),
				@StoredProcedureParameter(name = Car.RESULT_SET_PROC_ID_PARAM, mode = ParameterMode.IN, type = Integer.class),
				@StoredProcedureParameter(name = Car.RESULT_SET_PROC_TITLE_PARAM, mode = ParameterMode.IN, type = String.class)
			})
})
@SqlResultSetMapping(name = "carMapping", entities = { @EntityResult(entityClass = Car.class) })
public class Car {

	public static final String SIMPLE_VALUE_PROC = "simpleValueProcedure";
	public static final String UNIQUE_VALUE_PROC_PARAM = "param";

	public static final String RESULT_SET_PROC = "resultSetProcedure";
	public static final String RESULT_SET_PROC_ID_PARAM = "id";
	public static final String RESULT_SET_PROC_TITLE_PARAM = "title";

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
		return Objects.equals( id, car.id ) &&
				Objects.equals( title, car.title );
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
