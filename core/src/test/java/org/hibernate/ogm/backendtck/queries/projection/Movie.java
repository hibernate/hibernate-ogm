/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries.projection;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.SortableField;
import org.hibernate.search.annotations.Store;

@Entity
@Table(name = "Movie")
@Indexed
public class Movie {

	@Id
	private Long id;

	@Field(analyze = Analyze.NO, store = Store.YES)
	@SortableField
	private String name;

	@Field(analyze = Analyze.NO, store = Store.YES)
	@SortableField
	private String author;

	@Field(analyze = Analyze.NO, store = Store.YES)
	@SortableField
	private Integer year;

	public Movie() {
	}

	public Movie(Long id, String name, String author, int year) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.year = year;
	}

	public Long getId() {
		return id;
	}

	@Override
	public String toString() {
		return "Movie{" +
				"id=" + id +
				", name='" + name + '\'' +
				", author='" + author + '\'' +
				", year=" + year +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Movie movie = (Movie) o;
		return Objects.equals( id, movie.id ) &&
			Objects.equals( name, movie.name ) &&
			Objects.equals( author, movie.author ) &&
			Objects.equals( year, movie.year );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, name, author, year );
	}
}
