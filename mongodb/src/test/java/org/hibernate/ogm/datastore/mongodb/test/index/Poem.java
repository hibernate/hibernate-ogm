/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.index;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * @author Francois Le Droff
 */
@Entity
// @Table(name="POEM")
@Table(name = "T_POEM", indexes = @Index(columnList = "author ASC", name = "author_idx", unique = true) )
// @WriteConcern(WriteConcernType.ACKNOWLEDGED)
public class Poem {

	private String id;
	private String name;

	private String author;

	Poem() {
	}

	public Poem(String id, String name, String author) {
		this.id = id;
		this.name = name;
		this.author = author;
	}

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
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
}
