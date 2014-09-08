/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.persister;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Gunnar Morling
 */
@Entity
public class Eater {
	private String id;
	private String name;
	private Set<Pancake> pancakes = new HashSet<Pancake>();
	private Set<Muffin> muffins = new HashSet<Muffin>();
	private Set<Muffin> muffinsEatenAsStandin = new HashSet<Muffin>();

	public Eater() {
	}

	public Eater(String id) {
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

	@OneToMany(mappedBy = "eater")
	public Set<Pancake> getPancakes() {
		return pancakes;
	}

	public void setPancakes(Set<Pancake> pancakes) {
		this.pancakes = pancakes;
	}

	@OneToMany(mappedBy = "eater")
	public Set<Muffin> getMuffins() {
		return muffins;
	}

	public void setMuffins(Set<Muffin> muffins) {
		this.muffins = muffins;
	}

	@OneToMany(mappedBy = "standinEater")
	public Set<Muffin> getMuffinsEatenAsStandin() {
		return muffinsEatenAsStandin;
	}

	public void setMuffinsEatenAsStandin(Set<Muffin> muffinsEatenAsStandin) {
		this.muffinsEatenAsStandin = muffinsEatenAsStandin;
	}
}
