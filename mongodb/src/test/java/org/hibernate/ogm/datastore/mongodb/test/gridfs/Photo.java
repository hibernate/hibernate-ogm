/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.gridfs;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.ogm.datastore.mongodb.options.GridFSBucket;
import org.hibernate.ogm.datastore.mongodb.type.GridFS;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */

@Entity
public class Photo {

	public static final String BUCKET_NAME = "photos";

	@Id
	private String id;

	@GridFSBucket(BUCKET_NAME)
	private GridFS gridfs;

	private GridFS gridfsWithDefaultBucket;

	public Photo() {
	}

	public Photo(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public GridFS getGridFS() {
		return gridfs;
	}

	public void setGridFS(GridFS gridfs) {
		this.gridfs = gridfs;
	}

	public GridFS getGridfsWithDefaultBucket() {
		return gridfsWithDefaultBucket;
	}

	public void setGridfsWithDefaultBucket(GridFS gridfsWithDefault) {
		this.gridfsWithDefaultBucket = gridfsWithDefault;
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

	@Override
	public String toString() {
		return "Photo [id=" + id + "]";
	}
}
