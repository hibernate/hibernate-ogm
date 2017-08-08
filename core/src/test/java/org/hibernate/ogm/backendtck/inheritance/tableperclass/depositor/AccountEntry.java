/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.tableperclass.depositor;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "ACCOUNT_ENTRY")
public class AccountEntry {

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String id;

	@Column(name = "AMOUNT", updatable = false)
	@Basic(optional = false)
	private Double amount;

	protected AccountEntry() {
		// for JPA
	}

	public AccountEntry(final double amount) {
		this.amount = amount;
	}

	public double getAmount() {
		return amount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( amount == null ) ? 0 : amount.hashCode() );
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
		AccountEntry other = (AccountEntry) obj;
		if ( amount == null ) {
			if ( other.amount != null ) {
				return false;
			}
		}
		else if ( !amount.equals( other.amount ) ) {
			return false;
		}
		return true;
	}

}
