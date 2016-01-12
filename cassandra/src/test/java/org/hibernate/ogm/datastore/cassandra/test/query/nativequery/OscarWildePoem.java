/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.cassandra.test.query.nativequery;

import java.math.BigInteger;

import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;

@Entity
@Table(name = OscarWildePoem.TABLE_NAME,
		indexes = {
				@Index(name = "test_name_index", columnList = "name"),
				@Index(name = "test_author_index", columnList = "author"),
				@Index(name = "test_rating_index", columnList = "rating"),
				@Index(name = "test_score_index", columnList = "score")
		}
)

@NamedNativeQueries({
		@NamedNativeQuery(name = "AthanasiaQuery", query = "SELECT * FROM \"WILDE_POEM\" WHERE name='Athanasia'", resultClass = OscarWildePoem.class),

		@NamedNativeQuery(name = "AthanasiaQueryWithMapping", query = "SELECT * FROM \"WILDE_POEM\" WHERE name='Athanasia'", resultSetMapping = "poemMapping"),
		@NamedNativeQuery(name = "AthanasiaProjectionQuery", query = "SELECT * FROM \"WILDE_POEM\" WHERE name='Athanasia'", resultSetMapping = "poemNameMapping"),
		@NamedNativeQuery(name = "PoemRatings", query = "SELECT * FROM \"WILDE_POEM\" WHERE rating=2", resultSetMapping = "ratingMapping"),
		@NamedNativeQuery(name = "CountPoems", query = "SELECT count(*) AS \"n\" FROM \"WILDE_POEM\"", resultSetMapping = "countMapping")
})
@SqlResultSetMappings({
		@SqlResultSetMapping(name = "poemNameMapping", columns = @ColumnResult(name = "name")),
		@SqlResultSetMapping(name = "poemMapping", entities = @EntityResult(entityClass = OscarWildePoem.class)),
		@SqlResultSetMapping(
				name = "poemNameAuthorIdMapping",
				columns = {
						@ColumnResult(name = "name"),
						@ColumnResult(name = "author"),
						@ColumnResult(name = "id")
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
	private BigInteger score;

	public OscarWildePoem() {
	}

	public OscarWildePoem(Long id, String name, String author) {
		this.id = id;
		this.name = name;
		this.author = author;
	}

	public OscarWildePoem(Long id, String name, String author, byte rating, BigInteger score) {
		this.id = id;
		this.name = name;
		this.author = author;
		this.rating = rating;
		this.score = score;
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

	public BigInteger getScore() {
		return score;
	}

	public void setScore(BigInteger score) {
		this.score = score;
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
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		}
		else if ( !id.equals( other.id ) ) {
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
		if ( rating != other.rating ) {
			return false;
		}
		if ( score == null ) {
			if ( other.score != null ) {
				return false;
			}
		}
		else if ( !score.equals( other.score ) ) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( author == null ) ? 0 : author.hashCode() );
		result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
		result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
		result = prime * result + rating;
		result = prime * result + ( ( score == null ) ? 0 : score.hashCode() );
		return result;
	}

	@Override
	public String toString() {
		return "OscarWildePoem [id=" + id + ", name=" + name + ", author=" + author + ", rating=" + rating + ", score=" + score + "]";
	}
}
