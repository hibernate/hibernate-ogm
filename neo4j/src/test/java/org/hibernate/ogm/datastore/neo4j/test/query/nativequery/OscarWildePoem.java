/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.test.query.nativequery;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;

@Entity
@Table(name = OscarWildePoem.TABLE_NAME)
@NamedNativeQuery(name = "AthanasiaQuery", query = "MATCH ( n:" + OscarWildePoem.TABLE_NAME + " { name:'Athanasia', author:'Oscar Wilde' } ) RETURN n", resultClass = OscarWildePoem.class)
class OscarWildePoem {

	public static final String TABLE_NAME = "WILDE_POEM";

	private Long id;

	private String name;

	private String author;

	private Date dateOfCreation;

	public OscarWildePoem() {
	}

	public OscarWildePoem(Long id, String name, String author, Date dateOfCreation) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.dateOfCreation = dateOfCreation;
	}

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Date getDateOfCreation() {
		return dateOfCreation;
	}

	public void setDateOfCreation(Date dateOfCreation) {
		this.dateOfCreation = dateOfCreation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( author == null ) ? 0 : author.hashCode() );
		result = prime * result + ( ( dateOfCreation == null ) ? 0 : dateOfCreation.hashCode() );
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		return result;
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
		OscarWildePoem other = (OscarWildePoem) obj;
		if ( author == null ) {
			if ( other.author != null ) {
				return false;
			}
		}
		else if ( !author.equals( other.author ) ) {
			return false;
		}
		if ( dateOfCreation == null ) {
			if ( other.dateOfCreation != null ) {
				return false;
			}
		}
		else if ( !dateOfCreation.equals( other.dateOfCreation ) ) {
			return false;
		}
		if ( name == null ) {
			if ( other.name != null ) {
				return false;
			}
		}
		else if ( !name.equals( other.name ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "OscarWildePoem [id=" + id + ", name=" + name + ", author=" + author + "]";
	}

}
