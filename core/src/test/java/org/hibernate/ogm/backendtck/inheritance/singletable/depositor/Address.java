/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.singletable.depositor;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "ADDRESS")
public class Address {

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String id;


	@Column(name = "STREET", length = 50, updatable = false)
	@Basic(optional = false)
	private String street;

	@Column(name = "ZIP_CODE", length = 6, updatable = false)
	@Basic(optional = false)
	private String zipCode;

	@Column(name = "CITY", length = 25, updatable = false)
	@Basic(optional = false)
	private String city;

	@Column(name = "COUNTRY", length = 50, updatable = false)
	@Basic(optional = false)
	private String country;

	protected Address() {
		// for JPA
	}

	public Address(final String street, final String zipCode, final String city, final String country) {
		this.street = street;
		this.zipCode = zipCode;
		this.city = city;
		this.country = country;
	}

	public String getStreet() {
		return street;
	}

	public String getZipCode() {
		return zipCode;
	}

	public String getCity() {
		return city;
	}

	public String getCountry() {
		return country;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( city == null ) ? 0 : city.hashCode() );
		result = prime * result + ( ( country == null ) ? 0 : country.hashCode() );
		result = prime * result + ( ( street == null ) ? 0 : street.hashCode() );
		result = prime * result + ( ( zipCode == null ) ? 0 : zipCode.hashCode() );
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
		Address other = (Address) obj;
		if ( city == null ) {
			if ( other.city != null ) {
				return false;
			}
		}
		else if ( !city.equals( other.city ) ) {
			return false;
		}
		if ( country == null ) {
			if ( other.country != null ) {
				return false;
			}
		}
		else if ( !country.equals( other.country ) ) {
			return false;
		}
		if ( street == null ) {
			if ( other.street != null ) {
				return false;
			}
		}
		else if ( !street.equals( other.street ) ) {
			return false;
		}
		if ( zipCode == null ) {
			if ( other.zipCode != null ) {
				return false;
			}
		}
		else if ( !zipCode.equals( other.zipCode ) ) {
			return false;
		}
		return true;
	}

}
