/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.id.embeddedid;

import javax.persistence.Embeddable;
import java.io.Serializable;

/**
 * @author amorozov
 */
@Embeddable
public class AuthorId implements Serializable {

	private String id;

	public AuthorId(String id) {
		this.id = id;
	}

	protected AuthorId() {
	}

	public String getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AuthorId authorId = (AuthorId) o;
		return id.equals( authorId.id );

	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

}
