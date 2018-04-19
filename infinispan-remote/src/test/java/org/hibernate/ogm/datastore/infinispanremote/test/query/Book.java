package org.hibernate.ogm.datastore.infinispanremote.test.query;

import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.search.annotations.Indexed;

@Entity
@Indexed
public class Book {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
	@SequenceGenerator(name = "book_seq", sequenceName = "book_seq")
	private Integer id;

	private String code;

	private String author;

	private Integer year;

	private String title;

	public Book() {
	}

	public Book(String code, String author, Integer year, String title) {
		this.code = code;
		this.author = author;
		this.year = year;
		this.title = title;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Book book = (Book) o;
		return Objects.equals( code, book.code ) &&
			Objects.equals( author, book.author ) &&
			Objects.equals( year, book.year ) &&
			Objects.equals( title, book.title );
	}

	@Override
	public int hashCode() {
		return Objects.hash( code, author, year, title );
	}

	@Override
	public String toString() {
		return "Book{" +
			"id=" + id +
			", code='" + code + '\'' +
			", author='" + author + '\'' +
			", year=" + year +
			", title='" + title + '\'' +
			'}';
	}
}
