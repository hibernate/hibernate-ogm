/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.embeddedid;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * @author amorozov
 */
@Entity
public class Author {

	@EmbeddedId
	private AuthorId id;

	private String title;

	public Author(AuthorId id, String title) {
		this.id = id;
		this.title = title;
	}

	protected Author() {

	}

	public AuthorId getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

}
