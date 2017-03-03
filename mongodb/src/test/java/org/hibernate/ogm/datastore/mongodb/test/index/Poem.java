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

import org.hibernate.ogm.options.shared.IndexOption;
import org.hibernate.ogm.options.shared.IndexOptions;

/**
 * @author Francois Le Droff
 * @author Guillaume Smet
 */
@Entity
@Table(name = "T_POEM", indexes = {
		@Index(columnList = "author ASC", name = "author_idx"),
		@Index(columnList = "name DESC", name = "name_idx"),
		@Index(columnList = "author, name", name = "author_name_idx", unique = true),
		@Index(columnList = "author, name", name = "author_name_text_idx"),
		@Index(columnList = "", name = "index_with_no_keys_idx"),
		@Index(columnList = "name, author"), // index with no name: the name will be generated by ORM
})
@IndexOptions({
		@IndexOption(forIndex = "author_idx", options = "{ background : true, partialFilterExpression : { author: 'Verlaine' } }"),
		@IndexOption(forIndex = "name_idx", options = "{ expireAfterSeconds : 10 }"),
		@IndexOption(forIndex = "author_name_text_idx", options = "{ text: true, default_language : 'fr', weights : { author: 2, name: 5 } }"),
		@IndexOption(forIndex = "non_existing_idx", options = ""),
})
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
