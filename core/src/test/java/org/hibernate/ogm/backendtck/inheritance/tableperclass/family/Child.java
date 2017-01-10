/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.tableperclass.family;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

@Entity
@DiscriminatorValue("CHILD")
class Child extends Person {

	@OneToOne
	private Woman mother;

	@OneToOne
	private Man father;

	public Child() {
	}

	public Child(String name) {
		super( name );
	}

	public Man getFather() {
		return father;
	}

	public void setFather(Man father) {
		this.father = father;
	}

	public Woman getMother() {
		return mother;
	}

	public void setMother(Woman mother) {
		this.mother = mother;
	}
}
