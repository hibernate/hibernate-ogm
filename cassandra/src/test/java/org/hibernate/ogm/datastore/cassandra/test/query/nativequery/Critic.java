/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.test.query.nativequery;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Gunnar Morling
 */
@Entity
public class Critic {

	// TODO identical copies of this class also exist in the other backends (mongo,neo) that implement native query.
	// We should probably just bite the bullet and loft it into core's backendtck,
	// even though the tests themselves can't be shared.

	@Id
	private CriticId id;

	private String name;

	Critic() {
	}

	public Critic(CriticId id, String name) {
		this.id = id;
		this.name = name;
	}

	public CriticId getId() {
		return id;
	}

	public void setId(CriticId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
