/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.cascade;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Fabio Massimo Ercoli
 */
@Entity
public class Party {

	@Id
	private Integer id;

	private String name;

	private Date moment;

	private String location;

	public Party() {
	}

	public Party(Integer id, String name, Date moment, String location) {
		this.id = id;
		this.name = name;
		this.moment = moment;
		this.location = location;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Date getMoment() {
		return moment;
	}

	public String getLocation() {
		return location;
	}

	public void setName(String name) {
		this.name = name;
	}
}
