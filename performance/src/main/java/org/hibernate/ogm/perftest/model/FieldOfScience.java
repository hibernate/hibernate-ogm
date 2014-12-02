/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.perftest.model;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Gunnar Morling
 */
@Entity
public class FieldOfScience {

	@Id
	private int id;
	private String name;
	private double complexity;

	public FieldOfScience() {
	}

	public FieldOfScience(int id, String name, double complexity) {
		this.id = id;
		this.name = name;
		this.complexity = complexity;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getComplexity() {
		return complexity;
	}

	public void setComplexity(double complexity) {
		this.complexity = complexity;
	}
}
