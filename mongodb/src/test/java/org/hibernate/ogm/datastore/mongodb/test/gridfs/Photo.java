/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.gridfs;

import java.sql.Blob;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.ogm.datastore.mongodb.options.BinaryStorage;
import org.hibernate.ogm.datastore.mongodb.options.GridFSBucket;

import static org.hibernate.ogm.datastore.mongodb.options.BinaryStorageType.GRID_FS;
import static org.hibernate.ogm.datastore.mongodb.test.gridfs.GridFsTest.BUCKET_NAME;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */

@Entity
public class Photo {
	@Id
	private String id;

	private String description;

	@Lob
	@GridFSBucket(BUCKET_NAME)
	@BinaryStorage( GRID_FS )
	private Blob content;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Blob getContent() {
		return content;
	}

	public void setContent(Blob content) {
		this.content = content;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Photo photo = (Photo) o;
		return Objects.equals( id, photo.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}
}
