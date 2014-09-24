/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.objectid;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.bson.types.ObjectId;

/**
 * @author Gunnar Morling
 */
@Entity
public class Ingredient {

	private ObjectId id;
	private String name;
	private Set<Snack> containedIn = new HashSet<Snack>();

	Ingredient() {
	}

	Ingredient(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToMany(mappedBy = "ingredients")
	public Set<Snack> getContainedIn() {
		return containedIn;
	}

	public void setContainedIn(Set<Snack> containedIn) {
		this.containedIn = containedIn;
	}
}
