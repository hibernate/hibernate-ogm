/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.manytomany;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@Entity
public class BankAccount {
	private String id;
	private String accountNumber;
	private Set<AccountOwner> owners = new HashSet<AccountOwner>();

	public BankAccount() {

	}

	public BankAccount(String id) {
		this.id = id;
	}

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(String accountNumber) {
		this.accountNumber = accountNumber;
	}

	@ManyToMany(mappedBy = "bankAccounts")
	public Set<AccountOwner> getOwners() {
		return owners;
	}

	public void setOwners(Set<AccountOwner> owners) {
		this.owners = owners;
	}

}
