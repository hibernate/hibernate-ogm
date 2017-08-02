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

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Entity
@DiscriminatorValue("CHILD")
@Indexed
class Child extends Person {

	@Field(analyze = Analyze.NO)
	private String favouriteToy;

	@OneToOne
	private Woman mother;

	@OneToOne
	private Man father;

	public Child() {
	}

	public Child(String name, String favouriteToy) {
		super( name );
		this.favouriteToy = favouriteToy;
	}

	public String getFavouriteToy() {
		return favouriteToy;
	}

	public void setFavouriteToy(String favouriteToy) {
		this.favouriteToy = favouriteToy;
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
