/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.collection.manytomany;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.annotations.Cascade;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
@Entity
public class AccountOwner {
	private String id;
	private String sSN;
	private Set<BankAccount> bankAccounts = new HashSet<BankAccount>();

	public AccountOwner() {

	}

	public AccountOwner(String id) {
		this.id = id;
	}

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSSN() {
		return sSN;
	}

	public void setSSN(String sSN) {
		this.sSN = sSN;
	}

	@ManyToMany(cascade = CascadeType.PERSIST)
	@Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
	public Set<BankAccount> getBankAccounts() {
		return bankAccounts;
	}

	public void setBankAccounts(Set<BankAccount> bankAccounts) {
		this.bankAccounts = bankAccounts;
	}

}
