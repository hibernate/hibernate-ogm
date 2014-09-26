/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.objectid;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.bson.types.ObjectId;

/**
 * @author Gunnar Morling
 */
@Entity
public class Drink {

	private ObjectId id;
	private String name;

	Drink() {
	}

	Drink(ObjectId id, String name) {
		this.id = id;
		this.name = name;
	}

	@Id
	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
