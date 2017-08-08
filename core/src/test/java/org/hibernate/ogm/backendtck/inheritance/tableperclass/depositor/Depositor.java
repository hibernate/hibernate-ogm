/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.tableperclass.depositor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@Table(name = "DEPOSITOR")
@Indexed
public class Depositor {

	// persistence specific attributes

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String id;

	@Version
	@Column(name = "VERSION")
	private Long version;

	// entity attributes

	@Column(name = "NAME")
	@Basic(optional = false)
	@Field(analyze = Analyze.NO)
	private String name;

	@Column(name = "SURNAME")
	@Basic(optional = false)
	private String surname;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "DEPOSITOR_ID")
	private Set<Address> addresses = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@JoinColumn(name = "DEPOSITOR_ID")
	private Set<ContactDetail> contactDetails = new HashSet<>();

	@OneToMany(mappedBy = "depositor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private Set<Account> accounts = new HashSet<>();

	protected Depositor() {
		// for JPA
	}

	public Depositor(final String name, final String surname) {
		this.name = name;
		this.surname = surname;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(final String surname) {
		this.surname = surname;
	}

	public Set<Account> getAccounts() {
		return Collections.unmodifiableSet( accounts );
	}

	protected boolean addAccount(final Account account) {
		return accounts.add( account );
	}

	public boolean removeAccount(final Account account) {
		if ( accounts.remove( account ) ) {
			account.setDepositor( null );
			return true;
		}
		return false;
	}

	public boolean addAddress(final Address address) {
		return addresses.add( address );
	}

	public boolean removeAddress(final Address address) {
		return addresses.remove( address );
	}

	public Set<Address> getAddresses() {
		return Collections.unmodifiableSet( addresses );
	}

	public Set<ContactDetail> getContactDetails() {
		return Collections.unmodifiableSet( contactDetails );
	}

	public boolean addContactDetail(final ContactDetail contactDetail) {
		return contactDetails.add( contactDetail );
	}

	public boolean removeContactDetail(final ContactDetail contactDetail) {
		return contactDetails.remove( contactDetail );
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( accounts == null ) ? 0 : accounts.hashCode() );
		result = prime * result + ( ( addresses == null ) ? 0 : addresses.hashCode() );
		result = prime * result + ( ( contactDetails == null ) ? 0 : contactDetails.hashCode() );
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		result = prime * result + ( ( surname == null ) ? 0 : surname.hashCode() );
		result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
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
		Depositor other = (Depositor) obj;
		if ( accounts == null ) {
			if ( other.accounts != null ) {
				return false;
			}
		}
		else if ( !accounts.equals( other.accounts ) ) {
			return false;
		}
		if ( addresses == null ) {
			if ( other.addresses != null ) {
				return false;
			}
		}
		else if ( !addresses.equals( other.addresses ) ) {
			return false;
		}
		if ( contactDetails == null ) {
			if ( other.contactDetails != null ) {
				return false;
			}
		}
		else if ( !contactDetails.equals( other.contactDetails ) ) {
			return false;
		}
		if ( name == null ) {
			if ( other.name != null ) {
				return false;
			}
		}
		else if ( !name.equals( other.name ) ) {
			return false;
		}
		if ( surname == null ) {
			if ( other.surname != null ) {
				return false;
			}
		}
		else if ( !surname.equals( other.surname ) ) {
			return false;
		}
		if ( version == null ) {
			if ( other.version != null ) {
				return false;
			}
		}
		else if ( !version.equals( other.version ) ) {
			return false;
		}
		return true;
	}
}
