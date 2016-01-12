/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.onetoone;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Mark Paluch
 */
@Embeddable
public class PersonId implements Serializable {
	private String firstName;
	private String lastName;

	public PersonId() {
	}

	public PersonId(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof PersonId ) ) {
			return false;
		}

		PersonId personId = (PersonId) o;

		if ( firstName != null ? !firstName.equals( personId.firstName ) : personId.firstName != null ) {
			return false;
		}
		return !( lastName != null ? !lastName.equals( personId.lastName ) : personId.lastName != null );

	}

	@Override
	public int hashCode() {
		int result = firstName != null ? firstName.hashCode() : 0;
		result = 31 * result + ( lastName != null ? lastName.hashCode() : 0 );
		return result;
	}
}
