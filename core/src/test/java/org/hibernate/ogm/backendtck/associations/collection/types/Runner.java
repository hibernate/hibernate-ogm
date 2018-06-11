/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.backendtck.associations.collection.types;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@Entity
public class Runner {

	@EmbeddedId
	private RunnerId runnerId;
	private int age;

	public Runner() {
	}

	public Runner(String firstname, String lastname, int age) {
		this.runnerId = new RunnerId( firstname, lastname );
		this.age = age;
	}

	public RunnerId getRunnerId() {
		return runnerId;
	}

	public void setRunnerId(RunnerId runnerId) {
		this.runnerId = runnerId;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public int hashCode() {
		return Objects.hash( age, runnerId );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		Runner other = (Runner) obj;
		return Objects.equals( age, other.age ) && Objects.equals( runnerId, other.runnerId );
	}

	public static class RunnerId implements Serializable {

		private String firstname;
		private String lastname;

		public RunnerId() {
		}

		public RunnerId(String firstname, String lastname) {
			this.firstname = firstname;
			this.lastname = lastname;
		}

		public String getFirstname() {
			return firstname;
		}

		public void setFirstname(String firstname) {
			this.firstname = firstname;
		}

		public String getLastname() {
			return lastname;
		}

		public void setLastname(String lastname) {
			this.lastname = lastname;
		}

		@Override
		public int hashCode() {
			return Objects.hash( firstname, lastname );
		}

		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) {
				return true;
			}
			else if ( obj == null ) {
				return false;
			}
			else if ( getClass() != obj.getClass() ) {
				return false;
			}
			RunnerId other = (RunnerId) obj;
			return Objects.equals( firstname, other.firstname ) && Objects.equals( lastname, other.lastname );
		}
	}
}
