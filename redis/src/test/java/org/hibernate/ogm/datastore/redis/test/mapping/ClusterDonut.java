/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.test.mapping;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * An entity using a Redis Cluster hash tag of {@code zoomzoom}
 * @author Mark Paluch
 */
@Entity
@Table(name = "{zoomzoom}.ClusterDonut")
public class ClusterDonut {

	@Id
	private String id;

	private double radius;

	private Glaze glaze;

	private String alias;

	ClusterDonut() {

	}

	public ClusterDonut(String id, double radius, Glaze glaze, String alias) {
		this.id = id;
		this.radius = radius;
		this.glaze = glaze;
		this.alias = alias;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public Glaze getGlaze() {
		return glaze;
	}

	public void setGlaze(Glaze glaze) {
		this.glaze = glaze;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	enum Glaze {
		Sugar, Dark, Pink
	}

}
