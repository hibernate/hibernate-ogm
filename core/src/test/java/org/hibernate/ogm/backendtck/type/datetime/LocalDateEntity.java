/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type.datetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Owns {@link LocalDate} and {@link LocalDateTime} fields
 *
 * @author Fabio Massimo Ercoli
 */
@Entity
public class LocalDateEntity {

	@Id
	private Integer id;

	private String name;

	private LocalDate day;

	private LocalDateTime moment;

	public LocalDateEntity() {
	}

	public LocalDateEntity(Integer id, String name, LocalDateTime moment) {
		this.id = id;
		this.name = name;

		this.moment = moment;
		this.day = moment.toLocalDate();
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

	public LocalDate getDay() {
		return day;
	}

	public void setDay(LocalDate day) {
		this.day = day;
	}

	public LocalDateTime getMoment() {
		return moment;
	}

	public void setMoment(LocalDateTime moment) {
		this.moment = moment;
	}
}
