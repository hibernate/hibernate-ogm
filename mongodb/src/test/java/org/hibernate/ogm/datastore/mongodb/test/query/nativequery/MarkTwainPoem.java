/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = MarkTwainPoem.TABLE_NAME)
public class MarkTwainPoem {

	static final String TABLE_NAME = "MARK_TWAIN_POEM";

	@Id
	private Long id;

	private String name;

	private String author;

	MarkTwainPoem() {
	}

	public MarkTwainPoem(Long id, String name, String author) {
		this.id = id;
		this.name = name;
		this.author = author;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, author );
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		MarkTwainPoem other = (MarkTwainPoem) obj;
		return Objects.equals( name, other.name ) &&
				Objects.equals( author, other.author );
	}

	@Override
	public String toString() {
		return "MarkTwainPoem [" + id + ", " + name + ", " + author + "]";
	}
}
