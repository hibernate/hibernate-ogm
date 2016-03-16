/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.ogm.datastore.document.options.MapStorage;
import org.hibernate.ogm.datastore.document.options.MapStorageType;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@Entity
public class User {
	private String id;
	private Map<String, Address> addresses = new HashMap<String, Address>();
	private Map<String, PhoneNumber> phoneNumbers = new HashMap<>();
	private Map<Integer, PhoneNumber> phoneNumbersByPriority = new HashMap<>();
	private Map<String, PhoneNumber> alternativePhoneNumbers = new HashMap<>();
	private Set<String> nicknames = new HashSet<String>();

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@OneToMany
	@JoinTable(name = "User_Address")
	@MapKeyColumn(name = "addressType")
	public Map<String, Address> getAddresses() {
		return addresses;
	}

	public void setAddresses(Map<String, Address> addresses) {
		this.addresses = addresses;
	}

	@OneToMany
	public Map<String, PhoneNumber> getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(Map<String, PhoneNumber> phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	@OneToMany
	@MapStorage(MapStorageType.AS_LIST)
	@MapKeyColumn(name = "phoneType")
	@JoinTable(name = "AlTERNATIVE_PHONE_NUMBER")
	public Map<String, PhoneNumber> getAlternativePhoneNumbers() {
		return alternativePhoneNumbers;
	}

	public void setAlternativePhoneNumbers(Map<String, PhoneNumber> alternativePhoneNumbers) {
		this.alternativePhoneNumbers = alternativePhoneNumbers;
	}

	@OneToMany
	@MapKeyColumn(name = "priority")
	@JoinTable(name = "AlTERNATIVE_PRIORITY")
	public Map<Integer, PhoneNumber> getPhoneNumbersByPriority() {
		return phoneNumbersByPriority;
	}

	public void setPhoneNumbersByPriority(Map<Integer, PhoneNumber> phoneNumbersByPriority) {
		this.phoneNumbersByPriority = phoneNumbersByPriority;
	}

	@ElementCollection
	@JoinTable(name = "Nicks", joinColumns = @JoinColumn(name = "user_id"))
	public Set<String> getNicknames() {
		return nicknames;
	}

	public void setNicknames(Set<String> nicknames) {
		this.nicknames = nicknames;
	}
}
