/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.manytoone;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class SalesGuy {
	private String id;
	private String name;
	private SalesForce salesForce;

	public SalesGuy() {
	}

	public SalesGuy(String id) {
		this.id = id;
	}

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	@NotFound(action = NotFoundAction.IGNORE)
	public SalesForce getSalesForce() {
		return salesForce;
	}

	public void setSalesForce(SalesForce salesForce) {
		this.salesForce = salesForce;
	}
}
