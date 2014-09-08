/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.persister;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Gunnar Morling
 */
@Entity
public class Muffin {

	private String id;
	private String name;
	private Eater eater;
	private Eater standinEater;

	public Muffin() {
	}

	public Muffin(String id) {
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
	public Eater getEater() {
		return eater;
	}

	public void setEater(Eater eater) {
		this.eater = eater;
	}

	@ManyToOne
	public Eater getStandinEater() {
		return standinEater;
	}

	public void setStandinEater(Eater standinEater) {
		this.standinEater = standinEater;
	}
}
