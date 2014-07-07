/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query.nativequery;

import javax.persistence.ColumnResult;
import javax.persistence.Entity;
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
	@NamedNativeQuery(name = "AthanasiaProjectionQuery", query = "db.WILDE_POEM.find({ '$and' : [ { 'name' : 'Athanasia' }, { 'author' : 'Oscar Wilde' } ] })", resultSetMapping = "poemAuthorNameMapping" ),
	@NamedNativeQuery(name = "CountPoems", query = "db.WILDE_POEM.count()", resultSetMapping = "countMapping")
})
@SqlResultSetMappings({
	@SqlResultSetMapping(name = "poemAuthorNameMapping", columns = @ColumnResult(name = "name")),
	@SqlResultSetMapping(name = "countMapping", columns = @ColumnResult(name = "n"))
})
public class OscarWildePoem {

	public static final String TABLE_NAME = "WILDE_POEM";

	private Long id;

	private String name;

	private String author;

	public OscarWildePoem() {
	}

	public OscarWildePoem(Long id, String name, String author) {
		this.id = id;
		this.name = name;
		this.author = author;
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

	@Override
	public String toString() {
		return "OscarWildePoem [id=" + id + ", name=" + name + ", author=" + author + "]";
	}

}
