/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.objectid;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.bson.types.ObjectId;
import org.hibernate.ogm.backendtck.queries.AnEmbeddable;

/**
 * @author Davide D'Alto
 */
@Entity
public class EntityWithObjectIdAndEmbeddable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private ObjectId id;

	@Embedded
	private AnEmbeddable anEmbeddable;

	public EntityWithObjectIdAndEmbeddable() {
	}

	public EntityWithObjectIdAndEmbeddable(AnEmbeddable anEmbeddable) {
		this.anEmbeddable = anEmbeddable;
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public AnEmbeddable getAnEmbeddable() {
		return anEmbeddable;
	}

	public void setAnEmbeddable(AnEmbeddable details) {
		this.anEmbeddable = details;
	}
}
