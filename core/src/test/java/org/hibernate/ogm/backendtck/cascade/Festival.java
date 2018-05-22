/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.cascade;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

/**
 * @author Fabio Massimo Ercoli
 */
@Entity
public class Festival {

	@Id
	private String name;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<Party> parties = new ArrayList<>();

	public Festival() {
	}

	public Festival(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<Party> getParties() {
		return parties;
	}

	public void add(Party party) {
		this.parties.add( party );
	}
}
