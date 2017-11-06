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

import org.hibernate.ogm.datastore.mongodb.type.GeoPoint;
import org.hibernate.ogm.options.shared.IndexOption;
import org.hibernate.ogm.options.shared.IndexOptions;

@Entity
@Table(name = Restaurant.COLLECTION_NAME, indexes = {
		@Index(columnList = "location", name = "location_spatial_idx")
})
@IndexOptions(
		@IndexOption(forIndex = "location_spatial_idx", options = "{ _type: '2dsphere' }")
)
public class Restaurant {

	public static final String COLLECTION_NAME = "T_RESTAURANT";

	@Id
	private Long id;

	private String name;

	private GeoPoint location;

	public Restaurant() {
	}

	public Restaurant(Long id, String name, GeoPoint location) {
		this.id = id;
		this.name = name;
		this.location = location;
	}

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

	public GeoPoint getLocation() {
		return location;
	}

	public void setLocation(GeoPoint location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return "Restaurant [id=" + id + ", name=" + name + ", location=" + location + "]";
	}
}
