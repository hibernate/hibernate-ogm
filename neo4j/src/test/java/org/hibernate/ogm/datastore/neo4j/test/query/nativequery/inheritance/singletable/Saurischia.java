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
 * Saurischian dinosaurs are traditionally distinguished from ornithischian dinosaurs by their three-pronged pelvic
 * structure, with the pubis pointed forward.
 */
@Entity
@DiscriminatorValue(Dinosauria.SAURISCHIA_DISC)
public class Saurischia extends Dinosauria {

	private boolean carnivore;

	public Saurischia() {
	}

	public Saurischia(String name) {
		super( name );
	}

	public boolean isCarnivore() {
		return this.carnivore;
	}

	public void setCarnivore(boolean created) {
		this.carnivore = created;
	}
}
