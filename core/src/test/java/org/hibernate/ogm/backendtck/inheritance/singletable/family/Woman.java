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
@DiscriminatorValue("WOMAN")
class Woman extends Person {

	@OneToOne(mappedBy = "wife")
	private Man husband;

	@OneToMany(mappedBy = "mother")
	private List<Child> children = new ArrayList<>();

	public Woman() {
	}

	public Woman(String name) {
		super( name );
	}

	public Man getHusband() {
		return husband;
	}

	public void setHusband(Man husband) {
		this.husband = husband;
	}

	public List<Child> getChildren() {
		return children;
	}

	public void setChildren(List<Child> children) {
		this.children = children;
	}
}
