/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.manytomany;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Davide D'Alto
 */
@Entity
public class Student {

	@Id
	private String id;

	private String name;

	public Student() {
	}

	public Student(String id, String name) {
		super();
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Student student = (Student) o;
		return Objects.equals( id, student.id ) &&
				Objects.equals( name, student.name );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, name );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "Student{" );
		sb.append( "id='" ).append( id ).append( '\'' );
		sb.append( ", name='" ).append( name ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}
