/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.singletable.depositor;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(value = "GIRO_ACCOUNT")
public class GiroAccount extends Account {

	@Column(name = "CREDIT_LIMIT")
	@Basic(optional = false)
	private Double creditLimit;

	protected GiroAccount() {
	}

	public GiroAccount(final Depositor depositor) {
		this( depositor, 0.0d );
	}

	public GiroAccount(final Depositor depositor, final double creditLimit) {
		super( depositor );
		this.creditLimit = creditLimit;
	}

	public double getCreditLimit() {
		return creditLimit;
	}

	public void setCreditLimit(final double creditLimit) {
		this.creditLimit = creditLimit;
	}
}
