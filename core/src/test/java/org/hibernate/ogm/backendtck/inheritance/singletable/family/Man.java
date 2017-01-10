/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.singletable.family;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
@DiscriminatorValue("MAN")
class Man extends Person {

	@OneToOne
	private Woman wife;

	@OneToMany(mappedBy = "father")
	private List<Child> children = new ArrayList<>();

	public Man() {
	}

	public Man(String name) {
		super( name );
	}

	public Woman getWife() {
		return wife;
	}

	public void setWife(Woman wife) {
		this.wife = wife;
	}

	public List<Child> getChildren() {
		return children;
	}

	public void setChildren(List<Child> children) {
		this.children = children;
	}
}
