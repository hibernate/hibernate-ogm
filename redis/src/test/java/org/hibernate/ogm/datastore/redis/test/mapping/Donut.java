package org.hibernate.ogm.datastore.redis.test.mapping;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Mark Paluch
 */
@Entity
public class Donut {

	@Id
	private String id;

	private double radius;

	private Glaze glaze;

	Donut() {

	}

	public Donut(String id, double radius, Glaze glaze) {
		this.id = id;
		this.radius = radius;
		this.glaze = glaze;
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

	enum Glaze {
		Sugar, Dark, Pink
	}

}
