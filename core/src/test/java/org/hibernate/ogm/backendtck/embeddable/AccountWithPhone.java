/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Davide D'Alto
 */
@Entity
public class AccountWithPhone {

	@Id
	private String id;

	@Embedded
	private PhoneNumber phoneNumber;

	private String name;

	protected AccountWithPhone() {
	}

	public AccountWithPhone(String id, String name) {
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public PhoneNumber getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(PhoneNumber list) {
		this.phoneNumber = list;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
