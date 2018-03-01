/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.storedprocedures;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * This class is only use to test that an exception is thrown when calling a stored procedure that returns multiple
 * entities.
 *
 * @author Davide D'Alto
 */
@Entity
class Motorbike {

	@Id
	private Integer id;

	private String title;

	public Motorbike() {
	}

	public Motorbike(Integer id, String title) {
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
		Motorbike car = (Motorbike) o;
		return Objects.equals( id, car.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}

	@Override
	public String toString() {
		return "Motorbike{" + id + ", '" + title + '}';
	}
}
