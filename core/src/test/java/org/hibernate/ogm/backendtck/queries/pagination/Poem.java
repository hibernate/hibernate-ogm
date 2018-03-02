/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.pagination;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;

@Entity
@Table(name = Poem.TABLE_NAME)
@Indexed
public class Poem {

	public static final String TABLE_NAME = "POEM";

	@Id
	private Long id;

	@Field(analyze = Analyze.NO)
	@SortableField
	private String name;

	@Field(analyze = Analyze.NO)
	@SortableField
	private String author;

	@Field(analyze = Analyze.NO)
	@SortableField
	private Integer year;

	public Poem() {
	}

	public Poem(Long id, String name, String author, int year) {
		this( id, name, author, year, 0 );
	}

	public Poem(Long id, String name, String author, int year, int copiesSold) {
		this( id, name, author, year, copiesSold, (byte) 0 );
	}

	public Poem(Long id, String name, String author, int year, int copiesSold, byte rating, String... mediums) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.year = year;
	}

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

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	@Override
	public String toString() {
		return "Poem [" + id + ", " + name + ", " + author + "]";
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Poem that = (Poem) o;
		return Objects.equals( name, that.name ) &&
				Objects.equals( author, that.author ) &&
				Objects.equals( year, that.year );
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, author, year );
	}
}
