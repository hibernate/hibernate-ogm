/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQuery;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Indexed
@NamedQuery(name = Helicopter.BY_NAME, query = "FROM Helicopter WHERE name = :name")
public class Helicopter {

	public static final String BY_NAME = "HelicopterNameQuery";

	private String uuid;
	private String name;
	private String make;

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	public String getUUID() {
		return uuid;
	}

	public void setUUID(String uuid) {
		this.uuid = uuid;
	}

	@Field(analyze = Analyze.NO)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}
}
