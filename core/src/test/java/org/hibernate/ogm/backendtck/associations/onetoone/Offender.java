/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.onetoone;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.OneToOne;

/**
 * @author Mark Paluch
 */
@Entity
public class Offender {

	@EmbeddedId
	private PersonId id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumns({
			@JoinColumn(name = "firstName"),
			@JoinColumn(name = "lastName")
	})
	private Victim victim;


	public Offender() {
	}

	public Offender(PersonId id) {
		this.id = id;
	}

	public PersonId getId() {
		return id;
	}

	public void setId(PersonId id) {
		this.id = id;
	}

	public Victim getVictim() {
		return victim;
	}

	public void setVictim(Victim victim) {
		this.victim = victim;
	}
}
