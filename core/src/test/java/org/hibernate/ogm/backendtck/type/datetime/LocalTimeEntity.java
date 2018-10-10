/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.datetime;

import java.time.LocalTime;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Owns a {@link LocalTime} field
 *
 * @author Fabio Massimo Ercoli
 */
@Entity
public class LocalTimeEntity {

	@Id
	private Integer id;

	private String name;

	private LocalTime time;

	public LocalTimeEntity() {
	}

	public LocalTimeEntity(Integer id, String name, LocalTime time) {
		this.id = id;
		this.name = name;
		this.time = time;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalTime getTime() {
		return time;
	}

	public void setTime(LocalTime time) {
		this.time = time;
	}
}
