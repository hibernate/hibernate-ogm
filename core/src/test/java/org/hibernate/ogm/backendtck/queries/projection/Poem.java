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
@Table(name = Poem.TABLE_NAME)
@Indexed
public class Poem {

	public static final String TABLE_NAME = "POEM";

	@Id
	private Long id;

	@Field(analyze = Analyze.NO, store = Store.YES)
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
		return "Poem{" +
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
		Poem poem = (Poem) o;
		return Objects.equals( id, poem.id ) &&
				Objects.equals( name, poem.name ) &&
				Objects.equals( author, poem.author ) &&
				Objects.equals( year, poem.year );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, name, author, year );
	}
}
