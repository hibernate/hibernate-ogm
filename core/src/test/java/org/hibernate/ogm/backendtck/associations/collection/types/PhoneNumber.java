/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.types;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * @author Gunnar Morling
 */
@Entity
public class PhoneNumber {

	@EmbeddedId
	private PhoneNumberId id;
	private String description;

	public PhoneNumber() {
	}

	public PhoneNumber(PhoneNumberId id, String description) {
		this.id = id;
		this.description = description;
	}

	public PhoneNumberId getId() {
		return id;
	}

	public void setId(PhoneNumberId id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Embeddable
	public static class PhoneNumberId implements Serializable {
		private String countryCode;
		private int number;

		public PhoneNumberId() {
		}

		public PhoneNumberId(String countryCode, int number) {
			this.countryCode = countryCode;
			this.number = number;
		}

		public String getCountryCode() {
			return countryCode;
		}

		public void setCountryCode(String countryCode) {
			this.countryCode = countryCode;
		}

		public int getNumber() {
			return number;
		}

		public void setNumber(int number) {
			this.number = number;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ( ( countryCode == null ) ? 0 : countryCode.hashCode() );
			result = prime * result + number;
			return result;
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
			PhoneNumberId other = (PhoneNumberId) obj;
			if ( countryCode == null ) {
				if ( other.countryCode != null ) {
					return false;
				}
			}
			else if ( !countryCode.equals( other.countryCode ) ) {
				return false;
			}
			if ( number != other.number ) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "PhoneNumberId [countryCode=" + countryCode + ", number=" + number + "]";
		}
	}
}
