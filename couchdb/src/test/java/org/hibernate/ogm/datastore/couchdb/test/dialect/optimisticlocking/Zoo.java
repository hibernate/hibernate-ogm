/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.test.dialect.optimisticlocking;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

/**
 * @author Gunnar Morling
 */
@Entity
public class Zoo {

	private String id;
	private String name;
	private String revision;
	private Set<Animal> animals = new HashSet<Animal>();

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name = "_rev")
	@Version
	@Generated(GenerationTime.ALWAYS)
	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany
	public Set<Animal> getAnimals() {
		return animals;
	}

	public void setAnimals(Set<Animal> animals) {
		this.animals = animals;
	}

	@Override
	public String toString() {
		return "Zoo [id=" + id + ", name=" + name + ", revision=" + revision + ", animals=" + animals + "]";
	}
}
