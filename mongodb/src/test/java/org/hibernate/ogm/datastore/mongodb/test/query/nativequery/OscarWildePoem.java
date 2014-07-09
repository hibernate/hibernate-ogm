/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;

@Entity
@Table(name = OscarWildePoem.TABLE_NAME)
@NamedNativeQueries({
	@NamedNativeQuery(name = "AthanasiaQuery", query = "{ $and: [ { name : 'Athanasia' }, { author : 'Oscar Wilde' } ] }", resultClass = OscarWildePoem.class ),
	@NamedNativeQuery(name = "AthanasiaQueryWithMapping", query = "{ $and: [ { name : 'Athanasia' }, { author : 'Oscar Wilde' } ] }", resultSetMapping = "poemMapping" ),
	@NamedNativeQuery(name = "AthanasiaProjectionQuery", query = "db.WILDE_POEM.find({ '$and' : [ { 'name' : 'Athanasia' }, { 'author' : 'Oscar Wilde' } ] })", resultSetMapping = "poemNameMapping" ),
	@NamedNativeQuery(name = "PoemRatings", query = "db.WILDE_POEM.find({}, { 'rating' : 1 })", resultSetMapping = "ratingMapping" ),
	@NamedNativeQuery(name = "CountPoems", query = "db.WILDE_POEM.count()", resultSetMapping = "countMapping")
})
@SqlResultSetMappings({
	@SqlResultSetMapping(name = "poemNameMapping", columns = @ColumnResult(name = "name")),
	@SqlResultSetMapping(name = "poemMapping", entities = @EntityResult(entityClass = OscarWildePoem.class)),
	@SqlResultSetMapping(
			name = "poemNameAuthorIdMapping",
			columns = {
					@ColumnResult(name = "name"),
					@ColumnResult(name = "author"),
					@ColumnResult(name = "_id")
			}
	),
	@SqlResultSetMapping(name = "countMapping", columns = @ColumnResult(name = "n")),
	@SqlResultSetMapping(name = "ratingMapping", columns = @ColumnResult(name = "rating", type = byte.class))
})
public class OscarWildePoem {

	public static final String TABLE_NAME = "WILDE_POEM";

	private Long id;
	private String name;
	private String author;
	private byte rating;

	public OscarWildePoem() {
	}

	public OscarWildePoem(Long id, String name, String author) {
		this.id = id;
		this.name = name;
		this.author = author;
	}

	public OscarWildePoem(Long id, String name, String author, byte rating) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.rating = rating;
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

	public byte getRating() {
		return rating;
	}

	public void setRating(byte rating) {
		this.rating = rating;
	}

	@Override
	public String toString() {
		return "OscarWildePoem [id=" + id + ", name=" + name + ", author=" + author + ", rating=" + rating + "]";
	}
}
