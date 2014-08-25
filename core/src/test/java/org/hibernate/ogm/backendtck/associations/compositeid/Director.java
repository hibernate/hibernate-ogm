/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.compositeid;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;

/**
 * @author Gunnar Morling
 */
@Entity
public class Director {

	private String id;
	private String name;
	private Tournament directedTournament;
	private List<Tournament> attendedTournaments;

	Director() {
	}

	public Director(String id, String name, Tournament directedTournament) {
		this.id = id;
		this.name = name;
		this.directedTournament = directedTournament;
		this.attendedTournaments = new ArrayList<Tournament>();
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

	@OneToOne
	public Tournament getDirectedTournament() {
		return directedTournament;
	}

	public void setDirectedTournament(Tournament directedTournament) {
		this.directedTournament = directedTournament;
	}

	@ManyToMany
	public List<Tournament> getAttendedTournaments() {
		return attendedTournaments;
	}

	public void setAttendedTournaments(List<Tournament> attendedTournaments) {
		this.attendedTournaments = attendedTournaments;
	}
}
