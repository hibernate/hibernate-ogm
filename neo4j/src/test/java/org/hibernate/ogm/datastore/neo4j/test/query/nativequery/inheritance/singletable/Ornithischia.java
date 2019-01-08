/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query.nativequery.inheritance.singletable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Ornithischia is an extinct clade of mainly herbivorous dinosaurs characterized by a pelvic structure similar to that
 * of birds.
 */
@Entity
@DiscriminatorValue(Dinosauria.ORNITHISCHIA_DISC)
public class Ornithischia extends Dinosauria {

	private boolean herbivore;

	public Ornithischia() {
	}

	public Ornithischia(String name) {
		super( name );
	}

	public boolean isHerbivore() {
		return this.herbivore;
	}

	public void setHerbivore(boolean updated) {
		this.herbivore = updated;
	}
}
