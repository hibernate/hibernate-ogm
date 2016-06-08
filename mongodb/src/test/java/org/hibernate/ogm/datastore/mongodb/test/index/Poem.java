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

import org.hibernate.ogm.datastore.mongodb.options.MongoDBIndexOptions;
import org.hibernate.ogm.datastore.mongodb.options.MongoDBTextIndexOptions;
import org.hibernate.ogm.datastore.mongodb.options.MongoDBCollection;

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
		@Index(columnList = "name, author"), // index with no name
		@Index(columnList = "name", name = "second_text_idx"),
		@Index(columnList = "name", name = "invalid_partialFilterExpression_idx")
} )
@MongoDBCollection(indexOptions = {
		@MongoDBIndexOptions(forIndex = "author_idx", background = true, sparse = true, partialFilterExpression = "{ author: 'Verlaine' }"),
		@MongoDBIndexOptions(forIndex = "name_idx", expireAfterSeconds = 10),
		@MongoDBIndexOptions(forIndex = "author_name_text_idx", text = @MongoDBTextIndexOptions(defaultLanguage = "fr", weights = "{ 'author': 2, 'name': 5 }")),
		@MongoDBIndexOptions(forIndex = "non_existing_idx"),
		@MongoDBIndexOptions(forIndex = "second_text_idx", text = @MongoDBTextIndexOptions()),
		@MongoDBIndexOptions(forIndex = "invalid_partialFilterExpression_idx", partialFilterExpression = "invalid { filter")
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
