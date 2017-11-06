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
 * @author Guillaume Smet
 */
@Entity
@Table(name = OscarWildePoem.COLLECTION_NAME, indexes = {
		@Index(columnList = "name DESC", name = "name_idx"),
		@Index(columnList = "name", name = "name_text_idx")
} )
@IndexOptions({
		@IndexOption(forIndex = "name_text_idx", options = "{ _type: 'text', default_language : 'fr', weights : { name: 5 } }"),
})
public class OscarWildePoem {

	public static final String COLLECTION_NAME = "T_OSCAR_WILDE_POEM";

	private String id;

	private String name;

	OscarWildePoem() {
	}

	public OscarWildePoem(String id, String name) {
		this.id = id;
		this.name = name;
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
}
