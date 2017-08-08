/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.inheritance.singletable.family;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
class Family {

	@Id
	private String name;

	@OneToMany(mappedBy = "familyName", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Person> members = new ArrayList<>();

	public Family() {
	}

	public Family(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Person> getMembers() {
		return members;
	}

	public void setMembers(List<Person> members) {
		this.members = members;
	}

	public void add(Person person) {
		person.setFamilyName( this );
		members.add( person );
	}

	@Override
	public String toString() {
		return "Family [name=" + name + "]";
	}
}
