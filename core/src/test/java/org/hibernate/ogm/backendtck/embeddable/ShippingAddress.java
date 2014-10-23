/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

/**
 * @author Gunnar Morling
 */
@Embeddable
public class ShippingAddress {

	@Embedded
	private PhoneNumber phone;

	private String addressLine;

	ShippingAddress() {
	}

	public ShippingAddress(PhoneNumber phone, String addressLine) {
		this.phone = phone;
		this.addressLine = addressLine;
	}

	public PhoneNumber getPhone() {
		return phone;
	}

	public void setPhone(PhoneNumber phone) {
		this.phone = phone;
	}

	public String getAddressLine() {
		return addressLine;
	}

	public void setAddressLine(String addressLine) {
		this.addressLine = addressLine;
	}
}
