/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.compositeid;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * @author Gunnar Morling
 */
@Entity
public class Tournament {

	private TournamentId id;
	private String name;

	Tournament() {
	}

	public Tournament(TournamentId id, String name) {
		this.id = id;
		this.name = name;
	}

	@EmbeddedId
	public TournamentId getId() {
		return id;
	}

	public void setId(TournamentId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
