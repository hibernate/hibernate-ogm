/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.manytoone;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class SalesForce {
	private String id;
	private String corporation;
	private Set<SalesGuy> salesGuys = new HashSet<SalesGuy>();

	public SalesForce() {
	}

	public SalesForce(String id) {
		this.id = id;
	}

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}


	public String getCorporation() {
		return corporation;
	}

	public void setCorporation(String corporation) {
		this.corporation = corporation;
	}

	@OneToMany(mappedBy = "salesForce")
	public Set<SalesGuy> getSalesGuys() {
		return salesGuys;
	}

	public void setSalesGuys(Set<SalesGuy> salesGuys) {
		this.salesGuys = salesGuys;
	}
}
