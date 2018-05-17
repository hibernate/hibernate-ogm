/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.query.nativequery;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Fabio Massimo Ercoli
 */
@Entity
@Table(name = "Registry")
@NamedNativeQueries(
		@NamedNativeQuery(name = "JohnQuery", query = "from HibernateOGMGenerated.Registry where level > 3 and name = 'John Doe'", resultClass = Employee.class)
)
public class Employee implements Serializable {

	@Id
	private Long id;

	private String name;

	@Temporal(TemporalType.DATE)
	private Date start;

	private Integer level;

	private Employee() {
	}

	public Employee(Long id, String name, Date start, Integer level) {
		this.id = id;
		this.name = name;
		this.start = start;
		this.level = level;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Date getStart() {
		return start;
	}

	public Integer getLevel() {
		return level;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Employee employee = (Employee) o;
		return Objects.equals( id, employee.id ) &&
				Objects.equals( name, employee.name ) &&
				Objects.equals( start, employee.start ) &&
				Objects.equals( level, employee.level );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, name, start, level );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "Employee{" );
		sb.append( "id=" ).append( id );
		sb.append( ", name='" ).append( name ).append( '\'' );
		sb.append( ", start=" ).append( start );
		sb.append( ", level=" ).append( level );
		sb.append( '}' );
		return sb.toString();
	}
}
